package com.intellij.ml.llm.template.utils

import ai.grazie.client.common.logging.qualifiedName
import com.github.javaparser.JavaParser
import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.google.gson.JsonElement
import java.nio.file.Path

class JavaParsingUtils {
    companion object{

        fun parseFile(path: String) {
            val parsed = StaticJavaParser.parse(Path.of(path))
            parsed.findAll(MethodDeclaration::class.java)
                .forEach { println("method: ${it.signature}, static=${it.isStatic}") }
        }

        fun isMethodStatic(filePath: Path, methodSignature: MethodSignature): Boolean{
            val parsed = JavaParser(ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)).parse(filePath)
            return parsed.result.get().findAll(MethodDeclaration::class.java)
                .filter {
                    it.isStatic && methodSignature.compare(it.signature)
                }.isNotEmpty()
        }

        fun isMethodStatic(filePath: Path, methodSignature: String): Boolean{
            val signature = MethodSignature.getMethodSignatureParts(methodSignature) ?: return false
            return isMethodStatic(filePath, signature)
        }

        fun findFieldTypes(path: Path, className: String): List<ClassField> {
            val parsed = JavaParser(ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)).parse(path)
            return parsed.result.get()
                .findAll(ClassOrInterfaceDeclaration::class.java)
                .filter {
                    it.fullyQualifiedName.get() == className
                }
                .map {
                    it.fields.map {
                        ClassField(it.variables[0].nameAsString, it.elementType.asString(), it.toString())
                    }
                }.reduce { acc, classFields -> acc + classFields }
        }


    }
}