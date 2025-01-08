package com.example.contract_generator

import com.example.contract_generator.AttachmentMapper.Companion.createDirectoryPath
import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions
import jakarta.transaction.Transactional
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.util.*

interface AttachmentService {
    fun upload(multipartFile: MultipartFile): Attachment
    fun download(id: Long): ResponseEntity<*>
    fun preview(id: Long): ResponseEntity<*>
    fun findById(id: Long): Attachment
    fun convertDocxToPdf(docxFile: File): Attachment
}

interface ContractService {
    fun generateContract(contractRequestDto: ContractRequestDto): Contract
}

@Service
class AttachmentServiceImpl(private  val repository: AttachmentRepository) : AttachmentService {

    @Transactional
    override fun upload(multipartFile: MultipartFile): Attachment{
        val entity = AttachmentMapper.toEntity(multipartFile)
        val file = File(entity.path).apply {
            parentFile.mkdirs()
        }.absoluteFile

        try {
            multipartFile.transferTo(file)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        return repository.save(entity)
    }


    override fun download(id: Long): ResponseEntity<*> {
        val fileEntity = repository.findByIdAndDeletedFalse(id)?:throw AttachmentNotFound()

        return fileEntity.run{
            val inputStream = FileInputStream(path)
            val resource = InputStreamResource(inputStream)

            ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"${name}\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource)
        }
    }

    override fun preview(id: Long): ResponseEntity<*> {
        val fileEntity = repository.findByIdAndDeletedFalse(id)?:throw AttachmentNotFound()

        return fileEntity.run {
            val inputStream = FileInputStream(path)
            val resource = InputStreamResource(inputStream)

            ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"${name}\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource)
        }
    }

    override fun findById(id: Long): Attachment {
        val attachment = repository.findById(id)
        if (attachment.isPresent) {
            return attachment.get()
        }else throw AttachmentNotFound()
    }

    override fun convertDocxToPdf(docxFile: File): Attachment {

        val directory = docxFile.parentFile
        val uuid = docxFile.nameWithoutExtension
        val pdfFile = File(directory, "$uuid.pdf")

        FileInputStream(docxFile).use { doc ->
            XWPFDocument(doc).use { document ->
                FileOutputStream(pdfFile).use { out ->
                    PdfConverter.getInstance().convert(document, out, PdfOptions.create())
                }
            }
        }

        val attachment = Attachment(
            name = uuid,
            contentType = "application/pdf",
            size = pdfFile.length(),
            extension = "pdf",
            path = directory.absolutePath + "/$uuid.pdf"
        )
        return repository.save(attachment)
    }

}
@Service
class ContractServiceImpl(private val repository: AttachmentRepository,
    private val templateService: TemplateService,
    private val contractRepository: ContractRepository,
    private val clientRepository: ClientRepository,
    private val attachmentService: AttachmentService) : ContractService {
    @Transactional
    override fun generateContract(contractRequestDto: ContractRequestDto): Contract {

        val template = templateService.getTemplate(contractRequestDto.templateId)

        val attachment = attachmentService.download(template.file.id)
        val (_, uuid, path) = createDirectoryPath(subFolder = "contract")
        val tempFile = File("$path/$uuid.docx")
        Files.write(tempFile.toPath(), (attachment.body as ByteArray))

        val replacedFile = replaceKeysWithStyles(tempFile, contractRequestDto.keys)

        val clientKeyPrefix = "client."
        val clientFields = contractRequestDto.keys.filterKeys { it.startsWith(clientKeyPrefix) }
        val client = if (clientFields.isNotEmpty()) {
            val newClient = Client(
                fullName = clientFields["${clientKeyPrefix}fullName"]!!,
                pnfl = clientFields["${clientKeyPrefix}pnfl"]!!,
                passportId = clientFields["${clientKeyPrefix}passport_seria_num"]!!
            )
            clientRepository.save(newClient)
        } else null

        val pdfFile = attachmentService.convertDocxToPdf(replacedFile)


        val operator = SecurityContextHolder.getContext().authentication.principal as User
        val contract = Contract(
            file = pdfFile,
            client = client!!
        )
        contract.operators.add(operator)
        return contractRepository.save(contract)
    }

    private fun replaceKeysWithStyles(docxFile: File, keys: Map<String, String>): File {
        val wordDoc = XWPFDocument(Files.newInputStream(docxFile.toPath()))

        wordDoc.paragraphs.forEach { paragraph ->
            paragraph.runs.forEach { run ->
                keys.forEach { (key, value) ->
                    if (run.text().contains(key)) {
                        val style = run.fontFamily
                        val fontSize = run.fontSize
                        run.setText(run.text().replace(key, value), 0)
                        run.fontFamily = style
                        run.fontSize = fontSize
                    }
                }
            }
        }

        val replacedFile = File("replaced_template.docx")
        Files.newOutputStream(replacedFile.toPath()).use { wordDoc.write(it) }
        return replacedFile
    }




}
