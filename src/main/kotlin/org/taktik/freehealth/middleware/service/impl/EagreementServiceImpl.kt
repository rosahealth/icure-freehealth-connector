package org.taktik.freehealth.middleware.service.impl

import be.cin.encrypted.BusinessContent
import be.cin.encrypted.EncryptedKnownContent
import be.fgov.ehealth.agreement.protocol.v1.AskAgreementRequest
import be.fgov.ehealth.agreement.protocol.v1.AskAgreementResponse
import be.fgov.ehealth.agreement.protocol.v1.AskAgreementResponseType
import be.fgov.ehealth.agreement.protocol.v1.ConsultAgreementResponse
import be.fgov.ehealth.etee.crypto.utils.KeyManager
import be.fgov.ehealth.messageservices.mycarenet.core.v1.SendTransactionResponse
import be.fgov.ehealth.mycarenet.commons.core.v3.*
import be.fgov.ehealth.standards.kmehr.mycarenet.cd.v1.CDERRORMYCARENETschemes
import be.fgov.ehealth.technicalconnector.signature.AdvancedElectronicSignatureEnumeration
import be.fgov.ehealth.technicalconnector.signature.SignatureBuilderFactory
import be.fgov.ehealth.technicalconnector.signature.domain.SignatureVerificationError
import be.fgov.ehealth.technicalconnector.signature.domain.SignatureVerificationResult
import be.fgov.ehealth.technicalconnector.signature.transformers.EncapsulationTransformer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.apache.commons.codec.binary.Base64
import org.apache.commons.lang.StringUtils
import org.joda.time.DateTime
import org.json.JSONObject
import org.json.XML
import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.taktik.connector.business.agreement.exception.AgreementBusinessConnectorException
import org.taktik.connector.business.mycarenetcommons.builders.util.BlobUtil
import org.taktik.connector.business.mycarenetcommons.mapper.v3.BlobMapper
import org.taktik.connector.business.mycarenetdomaincommons.builders.BlobBuilderFactory
import org.taktik.connector.business.mycarenetdomaincommons.util.McnConfigUtil
import org.taktik.connector.business.mycarenetdomaincommons.util.PropertyUtil
import org.taktik.connector.technical.config.ConfigFactory
import org.taktik.connector.technical.exception.SoaErrorException
import org.taktik.connector.technical.exception.TechnicalConnectorException
import org.taktik.connector.technical.exception.TechnicalConnectorExceptionValues
import org.taktik.connector.technical.idgenerator.IdGeneratorFactory
import org.taktik.connector.technical.service.etee.Crypto
import org.taktik.connector.technical.service.etee.CryptoFactory
import org.taktik.connector.technical.service.etee.domain.EncryptionToken
import org.taktik.connector.technical.service.keydepot.KeyDepotService
import org.taktik.connector.technical.service.keydepot.impl.KeyDepotManagerImpl
import org.taktik.connector.technical.service.sts.security.Credential
import org.taktik.connector.technical.service.sts.security.impl.KeyStoreCredential
import org.taktik.connector.technical.utils.CertificateParser
import org.taktik.connector.technical.utils.ConnectorXmlUtils
import org.taktik.connector.technical.utils.IdentifierType
import org.taktik.connector.technical.utils.MarshallerHelper
import org.taktik.freehealth.middleware.dao.User
import org.taktik.freehealth.middleware.dto.mycarenet.CommonOutput
import org.taktik.freehealth.middleware.dto.mycarenet.MycarenetConversation
import org.taktik.freehealth.middleware.dto.mycarenet.MycarenetError
import org.taktik.freehealth.middleware.exception.MissingTokenException
import org.taktik.freehealth.middleware.service.EagreementService
import org.taktik.freehealth.middleware.service.STSService
import org.taktik.icure.fhir.entities.r4.bundle.Bundle
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.StringWriter
import java.util.*
import javax.xml.bind.JAXBContext
import javax.xml.datatype.DatatypeFactory
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMResult
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

@Service
class EagreementServiceImpl(private val stsService: STSService, private val keyDepotService: KeyDepotService) : EagreementService {
    private val freehealthAgreementService: org.taktik.connector.business.agreement.service.AgreementService = org.taktik.connector.business.agreement.service.impl.AgreementServiceImpl()

    private val keyDepotManager = KeyDepotManagerImpl.getInstance(keyDepotService)
    private val config = ConfigFactory.getConfigValidator(emptyList())

    val agreementServiceUtils: EagreementServiceUtilsImpl = EagreementServiceUtilsImpl();

    enum class RequestTypeEnum(val requestType: String) {
        ASK("claim-ask"),
        EXTEND("claim-extend"),
        ARGUE("claim-argue"),
        COMPLETE_AGREEMENT("claim-completeAgreement"),
        CANCEL("claim-cancel"),
        CONSULT_LIST("")
    }

    private val log = LoggerFactory.getLogger(this.javaClass)

    private fun generateError(e: AgreementBusinessConnectorException, co: CommonOutput): AskAgreementResponseType {
        val error = AskAgreementResponseType()
        error.isAcknowledged = false
        error.errors = Arrays.asList(MycarenetError(code = e.errorCode, msgFr = e.message, msgNl = e.message))
        error.commonOutput = co
        return error
    }

    private fun generateError(e: SoaErrorException): AskAgreementResponseType {
        val error = AskAgreementResponseType()
        error.isAcknowledged = false
        error.errors = Arrays.asList(MycarenetError(code = e.errorCode, msgFr = e.message, msgNl = e.message))
        return error
    }


    override fun askAgreement(
        keystoreId: UUID,
        tokenId: UUID,
        passPhrase: String,
        requestType: RequestTypeEnum,
        hcpQuality: String,
        messageEventSystem: String,
        messageEventCode: String,
        patientFirstName: String,
        patientLastName: String,
        patientGender: String,
        patientSsin: String?,
        patientIo: String?,
        patientIoMembership: String?,
        pathologyStartDate: DateTime,
        pathologyCode: String,
        insuranceRef: String,
        hcpNihii: String,
        hcpSsin: String,
        hcpFirstName: String,
        hcpLastName: String,
        orgNihii: String?,
        organizationType: String?,
        annex1: String?,
        annex2: String?,
        parameterNames: Array<String>?,
        agreementStartDate: DateTime?,
        agreementEndDate: DateTime?,
        agreementType: String?,
        numberOfSessionForAnnex1: Float?,
        numberOfSessionForAnnex2: Float?
    ): AskAgreementResponseType? {
        val samlToken =
            stsService.getSAMLToken(tokenId, keystoreId, passPhrase)
                ?: throw MissingTokenException("Cannot obtain token for Agreement operations")
        val keystore = stsService.getKeyStore(keystoreId, passPhrase)!!
        val credential = KeyStoreCredential(keystoreId, keystore, "authentication", passPhrase, samlToken.quality)
        val hokPrivateKeys = KeyManager.getDecryptionKeys(keystore, passPhrase.toCharArray())
        val crypto = CryptoFactory.getCrypto(credential, hokPrivateKeys)
        val detailId = "_" + IdGeneratorFactory.getIdGenerator("uuid").generateId()

        val now = DateTime.now().withMillisOfSecond(0)
        val xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(now.toGregorianCalendar())

        return extractEtk(credential)?.let {
            val requestBundle = createRequestBundle(
                requestType,
                messageEventSystem,
                messageEventCode,
                patientFirstName,
                patientLastName,
                patientGender,
                patientSsin,
                patientIo,
                patientIoMembership,
                pathologyStartDate,
                pathologyCode,
                insuranceRef,
                hcpNihii,
                hcpFirstName,
                hcpLastName,
                orgNihii,
                organizationType,
                annex1,
                annex2,
                parameterNames,
                agreementStartDate,
                agreementEndDate,
                agreementType,
                numberOfSessionForAnnex1,
                numberOfSessionForAnnex2
            )

            val askAgreementRequest = AskAgreementRequest().apply {
                val encryptedKnownContent = EncryptedKnownContent()
                encryptedKnownContent.replyToEtk = it.encoded
                val businessContent = BusinessContent().apply { id = detailId }
                encryptedKnownContent.businessContent = businessContent

                val requestJson = ObjectMapper().registerModule(KotlinModule()).writeValueAsString(requestBundle)
                val json = JSONObject(requestJson)
                val xmlString = "<Bundle xmlns=\"http://hl7.org/fhir\">" + XML.toString(json) + "</Bundle>"
                val requestXml = transformXml(xmlString)

                val byteArray = requestXml.toByteArray(Charsets.UTF_8)
                businessContent.value = byteArray

                log.info("Request is: " + businessContent.value.toString(Charsets.UTF_8))
                val xmlByteArray = handleEncryption(encryptedKnownContent, credential, crypto, detailId)

                val blob =
                    BlobBuilderFactory.getBlobBuilder("agreement")
                        .build(
                            xmlByteArray,
                            "none",
                            detailId,
                            "text/xml",
                            null as String?,
                            "3.0",
                            "encryptedForKnownBED"
                        )
                blob.messageName = "eAgreement-ask"

                val principal = SecurityContextHolder.getContext().authentication?.principal as? User
                val packageInfo = McnConfigUtil.retrievePackageInfo("agreement", principal?.mcnLicense, principal?.mcnPassword, principal?.mcnPackageName)

                commonInput = CommonInputType().apply {
                    request =
                        RequestType()
                            .apply {
                                isIsTest = config.getProperty("endpoint.agreement")?.contains("-acpt") ?: false
                            }
                    inputReference = inputReference
                    origin = OriginType().apply {
                        `package` = PackageType().apply {
                            license = LicenseType().apply {
                                username = packageInfo.userName
                                password = packageInfo.password
                            }
                            name = ValueRefString().apply { value = packageInfo.packageName }
                        }
                        config.getProperty("mycarenet.${PropertyUtil.retrieveProjectNameToUse("agreement", "mycarenet.")}.site.id")?.let {
                            if (it.isNotBlank()) {
                                siteID = ValueRefString().apply { value = it }
                            }
                        }
                        careProvider = CareProviderType().apply {
                            nihii =
                                NihiiType().apply {
                                    quality = hcpQuality;
                                    value = ValueRefString().apply { value = hcpNihii }
                                }
                            physicalPerson = IdType().apply {
                                name = ValueRefString().apply { value = "$hcpFirstName $hcpLastName" }
                                ssin = ValueRefString().apply { value = hcpSsin }
                                nihii = NihiiType().apply {
                                    quality = hcpQuality;
                                    value = ValueRefString().apply { value = hcpNihii }
                                }
                            }
                        }
                    }
                }
                routing = RoutingType().apply {
                    careReceiver = CareReceiverIdType().apply {
                        patientSsin?.let {
                            ssin = patientSsin
                        }
                        patientIoMembership?.let {
                            mutuality = patientIo
                            regNrWithMut = patientIoMembership
                        }
                    }
                    referenceDate = DateTime()
                }
                issueInstant = DateTime()
                this.detail = BlobMapper.mapBlobTypefromBlob(blob)
                this.id = IdGeneratorFactory.getIdGenerator("xsid").generateId()
                 //xades = BlobUtil.generateXades(credential, detail, "agreement")
            }

            try {
                val askAgreementResponse = freehealthAgreementService.askAgreement(samlToken, askAgreementRequest)

                val blobType = askAgreementResponse?.`return`?.detail
                val blob = BlobMapper.mapBlobfromBlobType(blobType!!)
                val unsealedData =
                    crypto.unseal(Crypto.SigningPolicySelector.WITHOUT_NON_REPUDIATION, blob.content).contentAsByte
                val decryptedKnownContent =
                    MarshallerHelper(EncryptedKnownContent::class.java, EncryptedKnownContent::class.java).toObject(
                        unsealedData)

                val xades = decryptedKnownContent!!.xades
                val signatureVerificationResult = xades?.let {
                    val builder = SignatureBuilderFactory.getSignatureBuilder(AdvancedElectronicSignatureEnumeration.XAdES)
                    val options = emptyMap<String, Any>()
                    builder.verify(unsealedData, it, options)
                } ?: SignatureVerificationResult().apply {
                    errors.add(SignatureVerificationError.SIGNATURE_NOT_PRESENT)
                }

                log.info("Response is: " + decryptedKnownContent.businessContent.value.toString(Charsets.UTF_8))

                val responseXML = decryptedKnownContent.businessContent.value.toString(Charsets.UTF_8)
                val responseJSON = XML.toJSONObject(responseXML)

                val errors = responseJSON.getJSONObject("Bundle").getJSONArray("entry")

                var commonOutput =
                    CommonOutput(
                        askAgreementResponse?.`return`?.commonOutput?.inputReference,
                        askAgreementResponse?.`return`?.commonOutput?.nipReference,
                        askAgreementResponse?.`return`?.commonOutput?.outputReference
                    )

                var res = AskAgreementResponseType()
                res.isAcknowledged = true
                res.commonOutput = commonOutput
                res.mycarenetConversation = MycarenetConversation().apply {
                    soapRequest = askAgreementResponse?.soapRequest?.writeTo(this.soapRequestOutputStream())?.toString()
                    soapResponse = askAgreementResponse?.soapResponse?.writeTo(this.soapResponseOutputStream())?.toString()
                    transactionRequest = ConnectorXmlUtils.toString(askAgreementRequest)
                    transactionResponse = responseJSON.toString()
                }
                res.content = responseJSON.toString()
                // TODO call that method but it's not fully implemented yest
                // res.errors = extractErrors(responseJSON).toList()
                return res;
            } catch (e: SoaErrorException) {
                throw TechnicalConnectorException(TechnicalConnectorExceptionValues.ERROR_WS, e, e.message)
            }

        }
    }

    override fun consultAgreement(keystoreId: UUID, tokenId: UUID, passPhrase: String): ConsultAgreementResponse? {
        TODO("Not yet implemented")
    }

    fun transformElement(element: Element, doc: Document) {
        val nodeList = element.childNodes
        for (i in 0 until nodeList.length) {
            val node = nodeList.item(i)
            if (node is Element) {
                transformElement(node, doc)
            }
        }
        if (element.childNodes.length == 1 && element.firstChild.nodeType == Node.TEXT_NODE) {
            val textContent = element.textContent
            element.textContent = "" // Clear the current text content
            element.setAttribute("value", textContent)
        }
    }


    // TODO check if this is needed. This transform request the same way we receive response : <xmlTag value="hello"/> instead of <xmlTag>hello</xmlTag>
    fun transformXml(inputXml: String): String {
        // Parse the XML
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        val originalDoc = dBuilder.parse(inputXml.byteInputStream())

        // Transform the document
        val rootElement = originalDoc.documentElement
        transformElement(rootElement, originalDoc)

        // Convert the document back to a string
        val transformerFactory = TransformerFactory.newInstance()
        val transformer = transformerFactory.newTransformer()
        val domSource = DOMSource(originalDoc)
        val writer = StringWriter()
        val result = StreamResult(writer)
        transformer.transform(domSource, result)

        return writer.toString()
    }


    fun createRequestBundle(
        requestType: RequestTypeEnum,
        messageEventSystem: String,
        messageEventCode: String,
        patientFirstName: String,
        patientLastName: String,
        patientGender: String,
        patientSsin: String?,
        patientIo: String?,
        patientIoMembership: String?,
        pathologyStartDate: DateTime,
        pathologyCode: String,
        insuranceRef: String,
        hcpNihii: String,
        hcpFirstName: String,
        hcpLastName: String,
        orgNihii: String?,
        organizationType: String?,
        annex1: String?,
        annex2: String?,
        parameterNames: Array<String>?,
        agreementStartDate: DateTime?,
        agreementEndDate: DateTime?,
        agreementType: String?,
        numberOfSessionForAnnex1: Float?,
        numberOfSessionForAnnex2: Float?
    ): Bundle?{

        val claim = this.agreementServiceUtils.getClaim(
            claimId = "1",
            claimStatus = "active",
            subTypeCode = "kine",
            agreementStartDate = DateTime(),
            insuranceRef = insuranceRef!!,
            pathologyCode = pathologyCode,
            pathologyStartDate = pathologyStartDate,
            providerType = ""
        )

        val bundle = this.agreementServiceUtils.getBundle(requestType, claim, messageEventSystem, messageEventCode, patientFirstName, patientLastName, patientGender, patientSsin, patientIo, patientIoMembership, hcpNihii, hcpFirstName, hcpLastName, orgNihii, organizationType, annex1, annex2, parameterNames, agreementStartDate, agreementEndDate, agreementType, numberOfSessionForAnnex1, numberOfSessionForAnnex2) ?: throw IllegalArgumentException("Cannot load fhir")
        return bundle
    }

    private fun extractEtk(cred: KeyStoreCredential): EncryptionToken? {
        val parser = CertificateParser(cred.certificate)
        if (parser.identifier != null && !StringUtils.isEmpty(parser.id) && StringUtils.isNumeric(parser.id)) {
            try {
                return KeyDepotManagerImpl.getInstance(keyDepotService)
                    .getEtk(parser.identifier, java.lang.Long.parseLong(parser.id), parser.application, cred.keystoreId, false)
            } catch (ex: NumberFormatException) {
                log.error(TechnicalConnectorExceptionValues.ERROR_ETK_NOTFOUND.message)
                throw TechnicalConnectorException(TechnicalConnectorExceptionValues.ERROR_ETK_NOTFOUND, ex)
            }
        } else {
            log.error(TechnicalConnectorExceptionValues.ERROR_ETK_NOTFOUND.message)
            throw TechnicalConnectorException(TechnicalConnectorExceptionValues.ERROR_ETK_NOTFOUND)
        }
    }

    private fun extractErrors(jsonObject: JSONObject): Set<MycarenetError> {
        val errors = mutableSetOf<MycarenetError>()
        val entries = jsonObject.getJSONObject("Bundle").getJSONArray("entry")

        for (i in 0 until entries.length()) {
            val entry = entries.getJSONObject(i)
            val resource = entry.getJSONObject("resource")

            if (resource.has("OperationOutcome")) {
                val operationOutcome = resource.getJSONObject("OperationOutcome")
                if (operationOutcome.has("issue")) {
                    val issue = operationOutcome.getJSONObject("issue")
                    if (issue.has("severity") && issue.getJSONObject("severity").getString("value") == "error") {
                        val errorCode = issue.getJSONObject("details").getJSONObject("coding").getJSONObject("code").getString("value")
                        errors.add(MycarenetError(code = errorCode))
                    }
                }
            }
        }

        return errors
    }

    private fun handleEncryption(
        request: EncryptedKnownContent,
        credential: Credential,
        crypto: Crypto,
        detailId: String
    ): ByteArray? {
        val marshaller = JAXBContext.newInstance(request.javaClass).createMarshaller()
        val res = DOMResult()

        marshaller.marshal(request, res)

        val doc = res.node as Document

        val nodes = doc.getElementsByTagNameNS("urn:be:cin:encrypted", "EncryptedKnownContent")
        val content = toStringOmittingXmlDeclaration(nodes)
        val builder = SignatureBuilderFactory.getSignatureBuilder(AdvancedElectronicSignatureEnumeration.XAdES)
        val options = HashMap<String, Any>()
        val tranforms = ArrayList<String>()
        tranforms.add("http://www.w3.org/2000/09/xmldsig#base64")
        tranforms.add("http://www.w3.org/2001/10/xml-exc-c14n#")
        options.put("transformerList", tranforms)
        options.put("baseURI", detailId)
        options.put("encapsulate", true)
        options.put("encapsulate-transformer", EncapsulationTransformer { signature ->
            val result = signature.ownerDocument.createElementNS("urn:be:cin:encrypted", "Xades")
            result.textContent = Base64.encodeBase64String(ConnectorXmlUtils.toByteArray(signature))
            result
        })
        val encryptedKnowContent = builder.sign(credential, content.toByteArray(charset("UTF-8")), options)
        return crypto.seal(
            Crypto.SigningPolicySelector.WITH_NON_REPUDIATION,
            KeyDepotManagerImpl.getInstance(keyDepotService).getEtkSet(
                IdentifierType.CBE,
                820563481L,
                "MYCARENET",
                null,
                false
            ),
            encryptedKnowContent
        )
    }

    @Throws(TransformerException::class)
    private fun toStringOmittingXmlDeclaration(nodes: NodeList): String {
        val sb = StringBuilder()
        val tf = TransformerFactory.newInstance()
        val serializer = tf.newTransformer()
        serializer.setOutputProperty("omit-xml-declaration", "yes")

        for (i in 0 until nodes.length) {
            val sw = StringWriter()
            serializer.transform(DOMSource(nodes.item(i)), StreamResult(sw))
            sb.append(sw.toString())
        }

        return sb.toString()
    }
}

