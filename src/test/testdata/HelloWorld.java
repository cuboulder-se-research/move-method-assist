package com.intellij.ml.llm.template.testdata;

import java.util.List;

class HelloWorld{
    public static void main(String[] args){
        int x =1;
        System.out.println(x);
        System.out.println("Hello world");

    }

    public void linearSearch(List<Integer> array, int value){
        for (int i=0;i<array.size();i++){
            if (array.get(i) ==value)
                System.out.println("Found value.");
        }
    }

    public void prettyPrintInteger(Integer num){
        switch (num){
            case 1: System.out.println("ONE!!"); break;
            case 2: System.out.println("TWO!!"); break;
            case 5: System.out.println("FIVE!!"); break;
            default: System.out.println(num);
        }
    }

}