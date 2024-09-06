package com.intellij.ml.llm.template.utils

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.MethodDeclaration
import java.nio.file.Path

class JavaParsingUtils {
    companion object{

        fun parseFile(path: String) {
            val parsed = StaticJavaParser.parse(Path.of(path))
            parsed.findAll(MethodDeclaration::class.java)
                .forEach { println("method: ${it.signature}, static=${it.isStatic}") }
        }


    }
}