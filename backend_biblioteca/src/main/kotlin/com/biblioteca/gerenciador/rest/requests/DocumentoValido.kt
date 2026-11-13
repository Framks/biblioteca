package com.biblioteca.gerenciador.rest.requests

import com.biblioteca.gerenciador.domain.Documento
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import kotlin.reflect.KClass

/**
 * Valida o documento de acordo com o tipo (RN14): CPF com dígitos verificadores, RG com tamanho plausível.
 * Fica na classe porque depende de dois campos (tipoDocumento + documento); o erro sai no campo "documento".
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [DocumentoValidoValidator::class])
annotation class DocumentoValido(
    val message: String = "documento inválido",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = [],
)

class DocumentoValidoValidator : ConstraintValidator<DocumentoValido, LeitorRequest> {

    override fun isValid(request: LeitorRequest?, contexto: ConstraintValidatorContext): Boolean {
        if (request == null || request.documento.isBlank()) return true // o @NotBlank já reporta

        val normalizado = Documento.normalizar(request.tipoDocumento, request.documento)
        if (Documento.valido(request.tipoDocumento, normalizado)) return true

        contexto.disableDefaultConstraintViolation()
        contexto.buildConstraintViolationWithTemplate("${request.tipoDocumento} inválido")
            .addPropertyNode("documento")
            .addConstraintViolation()
        return false
    }
}
