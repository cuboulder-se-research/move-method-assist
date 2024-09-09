package com.intellij.ml.llm.template.cli

import com.google.gson.Gson
import com.intellij.ml.llm.template.utils.JavaParsingUtils
import kotlinx.cli.*
import java.nio.file.Files
import kotlin.io.path.Path

enum class ProgramOptions{
    checkIfStatic,
    findFieldTypes
}

@ExperimentalCli
fun main(args: Array<String>){

    println("Welcome to my JavaParsing Utils CLI.")
    val parser = ArgParser("example")
    val input by parser.option(ArgType.String, shortName = "i", description = "Input file").required()
    val output by parser.option(ArgType.String, shortName = "o", description = "Output file name").required()
    class CheckIfStatic: Subcommand(ProgramOptions.checkIfStatic.name, "Check if method is static?") {
        val methodSignatureString by option(ArgType.String, "methodSignature", "s", "method signature").required()

        override fun execute() {
            Files.write(
                Path(output),
                JavaParsingUtils.isMethodStatic(
                    Path(input), methodSignatureString
                ).toString().toByteArray()
            )
        }
    }
    class FindFieldTypes: Subcommand("findFieldTypes", "Find field names and types for a given class") {
        val className by option(ArgType.String, shortName = "c", description = "Class name").required()
        override fun execute() {
            Files.write(
                Path(output),
                Gson().toJson(
                        JavaParsingUtils.findFieldTypes(Path(input), className)
                ).toByteArray()
            )
        }
    }
    val checkIfStatic = CheckIfStatic()
    val findFieldTypes = FindFieldTypes()
    parser.subcommands(checkIfStatic, findFieldTypes)
    parser.parse(args)

}