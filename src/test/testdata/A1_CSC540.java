/**
 * CSC 540-01 Assignment #1
 *
 * On my honor, ???, this assignment is my own work.
 * I, ???, will follow the instructor's rules and processes
 * related to academic integrity as directed in the course syllabus and assignment description.
 * I will not share my assignment with anyone else and make publicly available.
 *
 */


import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

// Students -- Add your import declarations here

public class A1_CSC540 {

    // Students -- Add your constants here

    public CharClass charClass;
    public String inputLine;
    public int inputLineIndex;
    public StringBuilder lexeme;
    public char x;
    public TokenCode nextToken;
    public boolean comment = false;

    enum CharClass {LETTER, DIGIT, UNKNOWN, ENDOFLINE}

    enum TokenCode {
        FLOATDCL(0),
        INTDCL(1),
        PRINT(2),
        ID(3),
        ASSIGN(4),
        PLUS(5),
        MINUS(6),
        INUM(7),
        FNUM(8),
        EOF(-1);

        public final int codeValue;

        private TokenCode(int value) {this.codeValue = value;}
    }

    class IllegalLexemeException extends RuntimeException {}
    class EndOfLineException extends RuntimeException {}


    public static void main(String[] args) {
        try {
            // Do NOT make any changes to the following TWO lines
            File file = new File(args[0]);
            Scanner sc = new Scanner(file);		//*** Do not make any other Scanners ***//

            // *** NOTE ***
            // For this assignment, you are NOT allowed to use any member methods of
            // class java.util.Scanner except hasNextLine and nextLine.
            // For example, you CANNOT use any of hasNextInt, nextInt, hasNextFloat, nextFloat,
            // hasNextDouble and nextDouble for this assignment.

            // Students -- Your code and methods calls go here

            A1 a1 = new A1();

            while (sc.hasNextLine() && a1.nextToken != TokenCode.EOF) {
                a1.readLine(sc.nextLine());
            }


            sc.close();
        } catch (FileNotFoundException e) {
            System.out.println("ERROR - cannot open i?.txt \n");
        }
    }

    // Students -- Add your method declarations here

    /** Start the lexical analysis process for each line.
     * 	First initialize the index to access each character to 0.
     *
     */
    void readLine(String line) {
        inputLineIndex = 0;
        inputLine = line;

        getChar();

        while (true) {
            try {

                parseNewLexeme();

            } catch (EndOfLineException e) {

                // Stop looping once we reach the end of the line.
                break;

            } catch (IllegalLexemeException e) {

                // Stop compilation immediately upon detection of lexical error.
                nextToken = TokenCode.EOF;
                break;

            }
        }
    }


    /* Grabs the next character in the string and sets it to nextChar,
     * then classify the character in charClass and increment the lineIndex.
     */
    void getChar() {

        if (inputLineIndex < inputLine.length()) {
            x = inputLine.charAt(inputLineIndex);

            if (Character.getType(x) == Character.LOWERCASE_LETTER) {
                charClass = CharClass.LETTER;
            }

            else if (Character.getType(x) == Character.DECIMAL_DIGIT_NUMBER ||
                    x == '.') {
                charClass = CharClass.DIGIT;
            }

            else {
                charClass = CharClass.UNKNOWN;
            }

            inputLineIndex++;

        } else {
            x = 0;
            charClass = CharClass.ENDOFLINE;
        }

    }

    /* Method chain. Right now, we just run the filter comments
     * method before running lex().
     */
    void parseNewLexeme() {

        filterComments();
        lex();

    }

    /* Filter out comments before running lex(). If getChar (or this function)
     * sets charClass to ENDOFLINE, this function will pass it
     * on to lex().
     */
    void filterComments() {

        getNonBlank();

        // Search for comments by first detecting the '/' character
        if (x == '/' && !comment) {
            getChar();
            if (x == '/') {		// Line comment
                charClass = CharClass.ENDOFLINE;  // After setting this, the rest of the line will be skipped
            } else if (x == '*') {			// Block comment
                comment = true;
                getChar();
            }
        }

        // If we are inside a block comment
        if (comment) {

            do {
                if (x == '*') {
                    getChar();
                    if (x == '/') {
                        comment = false;
                    }
                }
                getChar();
            } while (comment && charClass != CharClass.ENDOFLINE);

        }

    }

    /* 	Creates a new stringbuilder to hold the lexeme.
        Sets nextToken to classify the lemexe.
        Terminates without printing anything if charClass is EndOfLine  */
    TokenCode lex() {

        getNonBlank();

        lexeme = new StringBuilder();

        switch(charClass) {
            case LETTER -> {

                switch(x) {
                    case 'f' -> nextToken = TokenCode.FLOATDCL;
                    case 'i' -> nextToken = TokenCode.INTDCL;
                    case 'p' -> nextToken = TokenCode.PRINT;
                    default -> nextToken = TokenCode.ID;
                }

                addChar();
                getChar();

            }

            case DIGIT -> {

                /* We need to throw an error for three cases:
                 * 1. The number starts with a decimal point
                 * 2. The number ends with a decimal point
                 * 3. The number contains more than one decimal point
                 */

                if (x == '.') reportError();	 // handle case 1
                int decimalCount = 0;

                do {
                    addChar();
                    getChar();
                    if (x == '.') {
                        if (decimalCount == 1) reportError();  // handle case 3
                        else decimalCount++;
                    }
                } while (charClass == CharClass.DIGIT);

                if (x == '.') reportError();  // handle case 2

                if (decimalCount == 0) nextToken = TokenCode.INUM;
                else nextToken = TokenCode.FNUM;

            }

            case UNKNOWN -> {

                lookup();
                getChar();

            }

            case ENDOFLINE -> {
                throw new EndOfLineException();
            }

        }

        System.out.printf("Next token is: %d, Next lexeme is %s\n", nextToken.codeValue, lexeme.toString());

        return nextToken;
    }

    /* Throw exception on encountering illegal lexeme.
     * This will terminate the compilation process.
     */
    void reportError() {
        throw new IllegalLexemeException();
    }

    /* Add a character to the lexeme */
    void addChar() {
        lexeme.append(x);
    }

    /* Call getChar in a loop until a non-whitespace character is found */
    void getNonBlank() {
        while (Character.isWhitespace(x)) {
            getChar();
        }
    }

    /* Look up single-character tokens. If a character cannot be
     * identified, it hits the 'default' case and an error is reported.
     */
    TokenCode lookup() {
        nextToken = switch(x) {
            case '=' -> TokenCode.ASSIGN;
            case '+' -> TokenCode.PLUS;
            case '-' -> TokenCode.MINUS;
            default -> {
                reportError();  // Stop compiling on detection of illegal character
                yield TokenCode.EOF;
            }
        };


        addChar();

        return nextToken;
    }

}