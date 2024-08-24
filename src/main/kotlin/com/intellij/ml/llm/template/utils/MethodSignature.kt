package com.intellij.ml.llm.template.utils

import java.util.regex.Pattern

data class Parameter(val name: String, val type: String)

data class MethodSignature(val methodName: String, val paramsList: List<Parameter>, val returnType:String, val modifier: String){
    companion object{
        fun getMethodSignatureParts(methodSignature: String): MethodSignature? {
            val methodSignatureRegex = Pattern.compile("(\\w+) (\\w+)(\\(.*\\)) : (\\w+)")
            var m = methodSignatureRegex.matcher(methodSignature)
            if (m.find()) {
//            val methodInformation = methodSignature.strip().split(" ")
                val modifier = m.group(1)
//            val methodNameAndParams = methodInformation[1]
                val methodName = m.group(2)
                val methodParams = m.group(3)
                val returnType = m.group(4)
                return MethodSignature(methodName, getParamsList(methodParams), returnType, modifier)
            }
            return null
        }

        fun getParamsList(methodParams: String): List<Parameter>{
            if (methodParams=="()")
                return emptyList()
            return methodParams.substring(1, methodParams.length-1).split(",")
                .map {
                    val nameAndType = it.strip().split(" ")
                    Parameter(nameAndType[0], nameAndType[1])
                }
        }
    }

}



