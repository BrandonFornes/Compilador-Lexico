package com.example;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.*;
import java.util.List;

public class AnalizadorLexico extends JFrame { 

    // ─── Colores del tema (Simplificados) ───────────────────────────────────
    private static final Color BG_DARK       = new Color(18, 20, 28);
    private static final Color BG_CODE       = new Color(20, 23, 32);
    private static final Color BG_TABLE      = new Color(22, 25, 36);
    private static final Color TEXT_MAIN     = new Color(226, 232, 240);
    private static final Color LINE_NUM_BG   = new Color(30, 34, 48);
    private static final Color LINE_NUM_FG   = new Color(74, 85, 104);

    // ─── Componentes principales ────────────────────────────────────────────
    private JTable codeTable;
    private DefaultTableModel codeTableModel;
    private DefaultTableModel errorTableModel;
    private DefaultTableModel tokenTableModel;
    private DefaultTableModel counterTableModel; 
    private JLabel fileNameLabel;

    private ArrayList<Object[]> listaTokens = new ArrayList<>();
    private ArrayList<Object[]> listaErrores = new ArrayList<>();
    private ArrayList<Object[]> listaErroresSintactico = new ArrayList<>();
    private ArrayList<Object[]> ListaCambiosDeclaracion = new ArrayList<>();
    private ArrayList<Object[]> ListaCambiosAmbito = new ArrayList<>();
    private Stack<Integer> pilaAmbito = new Stack<>();
    private boolean EsDeclaracion = true;
    private static String[][] matriz;
    private static String[][] matrizSintactica;
    private Map<String, Integer> contadorNoTerminales = new HashMap<>();
    Stack<String> pilaSintactica = new Stack<>();
    private int[] categorias = new int[33];
    private final int ERRORES                        = 0;
    private final int ID_CADENA                       = 1;
    private final int ID_NUMERICA_BINARIO             = 2;
    private final int ID_NUMERICA_DECIMAL             = 3;
    private final int ID_NUMERICA_OCTAL               = 4;
    private final int ID_NUMERICA_HEXADECIMAL         = 5;
    private final int ID_REAL                         = 6;
    private final int ID_EXPONENCIAL                  = 7;
    private final int ID_BOOLEANAS                    = 8;
    private final int ID_REGISTRO                      = 9;
    private final int COMENTARIOS                     = 10;
    private final int PALABRAS_RESERVADAS             = 11;
    private final int CONSTANTES_CADENA               = 12;
    private final int CONSTANTES_NUMERICA_BINARIO     = 13;
    private final int CONSTANTES_NUMERICA_DECIMAL     = 14;
    private final int CONSTANTES_NUMERICA_OCTAL       = 15;
    private final int CONSTANTES_NUMERICA_HEXADECIMAL = 16;
    private final int CONSTANTES_REAL                 = 17;
    private final int CONSTANTES_EXPONENCIAL          = 18;
    private final int CONSTANTES_BOOLEANAS            = 19;
    private final int CONSTANTES_NULA                 = 20;
    private final int OPERADORES_POSTFIX              = 21;
    private final int OPERADORES_LOGICOS_BINARIOS     = 22;
    private final int OPERADORES_CONTROL              = 23;
    private final int OPERADORES_MATEMATICOS          = 24;
    private final int OPERADOR_EXPONENTE              = 25;
    private final int OPERADORES_TURNO                = 26;
    private final int OPERADORES_RELACIONALES         = 27;
    private final int OPERADORES_SIN_IGUALDAD         = 28;
    private final int OPERADORES_LOGICOS              = 29;
    private final int OPERADOR_TERNARIO               = 30;
    private final int OPERADORES_ASIGNACION           = 31;
    private final int OPERADORES_AGRUPAMIENTO         = 32;
    

    private final String[] nombresGrupos = {
    "ERRORES", "ID_CADENA", "ID_NUMERICA_BINARIO", "ID_NUMERICA_DECIMAL",
    "ID_NUMERICA_OCTAL", "ID_NUMERICA_HEXADECIMAL", "ID_REAL", "ID_EXPONENCIAL",
    "ID_BOOLEANAS","ID_REGISTRO", "COMENTARIOS", "PALABRAS_RESERVADAS", "CONSTANTES_CADENA",
    "CONSTANTES_NUMERICA_BINARIO", "CONSTANTES_NUMERICA_DECIMAL", "CONSTANTES_NUMERICA_OCTAL",
    "CONSTANTES_NUMERICA_HEXADECIMAL", "CONSTANTES_REAL", "CONSTANTES_EXPONENCIAL",
    "CONSTANTES_BOOLEANAS", "CONSTANTES_NULA", "OPERADORES_POSTFIX",
    "OPERADORES_LOGICOS_BINARIOS", "OPERADORES_CONTROL", "OPERADORES_MATEMATICOS",
    "OPERADOR_EXPONENTE", "OPERADORES_TURNO", "OPERADORES_RELACIONALES",
    "OPERADORES_SIN_IGUALDAD", "OPERADORES_LOGICOS", "OPERADOR_TERNARIO",
    "OPERADORES_ASIGNACION", "OPERADORES_AGRUPAMIENTO"
    };
    

    ArrayList<String> palabrasReservadas = new ArrayList<>(Arrays.asList(
        "if", "else", "switch", "for", "do", "while", "forEach", "break", "continue", "let", "const",
        "undefined", "interface", "typeof", "any", "interface", "set", "get", "class", "toLowerCase",
        "toUpperCase", "length", "trim", "charAt", "startsWith", "endsWith", "indexOf", "Includes","slice",
        "replace", "split", "push", "shift", "in", "of", "splice", "concat", "find", "findIndex", "filter", "map", "sort",
        "reverse", 
        "main","Console.read","Console.log","def","elseif","default","return","case","var","reg",
        "CLEAR", "SQRT", "POW", "SQRTV", "STRLEN","copy", "val", "str", "sin", "cos", "tan","chr","pred", "succ",
        "inc", "dec","sqr"
    ));
    ArrayList<String> booleanas = new ArrayList<>(Arrays.asList(
        "true", "false"
    ));
    ArrayList<String> nulas = new ArrayList<>(Arrays.asList(
        "null"
    ));
    private static final String[][] producciones = {
        {},
        {"A1", "main", "(", ")","800", "{", "STATU", "A2","801", "}"},
        {";", "STATU", "A2"},
        {"reg", "id", "{", "id", "A3", "}", "A1"},
        {",", "id", "A3"},
        {"var", "A4", "id", "A6", "A7", ";", "A1"},
        {"reg", "id"},
        {",", "const decimal", "A5"},
        {"[", "const decimal", "A5", "]"},
        {",", "A4", "id", "A6", "A7"},
        {"def", "id","802" ,"LISTA DE PARAMETROS", "PROGRAMA","803", ";", "A1"},
        {"id", "=", "DECLARACION CONSTANTES", "A8", ";", "A1"},
        {",", "id", "=", "DECLARACION CONSTANTES", "A8"}, 
        {"ε"},
        {"(", "id", "A3", ")"},
        {"CONSTANTE S/SIGNO"},
        {"+", "CONSTANTE S/SIGNO"},
        {"-", "CONSTANTE S/SIGNO"},
        {"const real"},
        {"const cadena"},
        {"CONST NUMERICA"},
        {"true"},
        {"false"},
        {"const exponencial"},
        {"null"},
        {"binario"},
        {"const decimal"},
        {"const octal"},
        {"const hexadecimal"},
        {"Console.read", "(", "OR", "B1", ")"},
        {",", "OR", "B1"},
        {"Console.log", "(", "OR", ")"},
        {"if", "(", "OR", ")", "STATU", "B2"},
        {"elseif", "(", "OR", ")", "STATU", "B2"},
        {"else", "STATU"},
        {"OR"},
        {"{", "STATU", "A2", "}"},
        {"while", "(", "OR", ")", "STATU"},
        {"do", "STATU", "while", "(", "OR", ")"},
        {"return", "OR"},
        {"for", "(", "OR", "B3", ")", "STATU"},
        {",", "OR", "B3"},
        {":", "OR"},
        {";", "STATU", ";", "OR", "B1"},
        {"switch", "(", "OR", ")", "{", "case", "OR", ":", "STATU", "B4", "}"},
        {";", "STATU", "B4"},
        {"break", "B5"},
        {"case", "OR", ":", "STATU", "B4"},
        {"default", ":", "STATU", "A2"},
        {"AND", "C1"},
        {"||", "AND", "C1"},
        {"|", "AND", "C1"},
        {"EXP_PAS", "D1"},
        {"&&", "EXP_PAS", "D1"},
        {"&", "EXP_PAS", "D1"},
        {"SIMPLE EXP PASCAL", "E1"},
        {"<", "SIMPLE EXP PASCAL", "E1"},
        {">=", "SIMPLE EXP PASCAL", "E1"},
        {"<=", "SIMPLE EXP PASCAL", "E1"},
        {"!=", "SIMPLE EXP PASCAL", "E1"},
        {"==", "SIMPLE EXP PASCAL", "E1"},
        {">", "SIMPLE EXP PASCAL", "E1"},
        {"TERMINO PASCAL", "F1"}, 
        {"-", "TERMINO PASCAL", "F1"},
        {"+", "TERMINO PASCAL", "F1"},
        {"<<", "TERMINO PASCAL", "F1"},
        {">>", "TERMINO PASCAL", "F1"},
        {">>>", "TERMINO PASCAL", "F1"}, // Índice 67: >>> TERMINO PASCAL F1
        {"ELEVACION", "G1"}, // Índice 68: ELEVACION G1
        {"*", "ELEVACION", "G1"}, // Índice 69: * ELEVACION G1
        {"/", "ELEVACION", "G1"}, // Índice 70: / ELEVACION G1
        {"#", "ELEVACION", "G1"}, // Índice 71: # ELEVACION G1
        {"&", "ELEVACION", "G1"}, // Índice 72: & ELEVACION G1
        {"%", "ELEVACION", "G1"}, // Índice 73: % ELEVACION G1
        {"FACTOR", "H1"}, // Índice 74: FACTOR H1
        {"^", "FACTOR", "H1"}, // Índice 75: ^ FACTOR H1
        {"DECLARACION CONSTANTES"}, // Índice 76: DECLARACION CONSTANTES
        {"id", "I1"}, // Índice 77: id I1
        {"ARR", "I2"}, // Índice 78: ARR I2
        {"ASIG", "OR", "I3"}, // Índice 79: ASIG OR I3
        {"(", "I4", ")"}, // Índice 80: ( I4 )
        {"ASIG", "OR", "I3"}, // Índice 81: ASIG OR I3
        {"?", "OR", ":", "OR"}, // Índice 82: ? OR : OR
        {"OR", "B1"}, // Índice 83: OR B1
        {"++", "id", "I1"}, // Índice 84: ++ id I1
        {"--", "id", "I1"}, // Índice 85: -- id I1
        {"(", "OR", ")"}, // Índice 86: ( OR )
        {"!", "(", "OR", ")"}, // Índice 87: ! ( OR )
        {"~", "(", "OR", ")"}, // Índice 88: ~ ( OR )
        {"FUNCION"}, // Índice 89: FUNCION
        {"[", "OR", "B1", "]"}, // Índice 90: [ OR B1 ]
        {"CLEAR"}, // Índice 91: CLEAR
        {"SQRT", "(", "OR", ")"}, // Índice 92: SQRT ( OR )
        {"POW", "(", "OR", ",", "OR", ")"}, // Índice 93: POW ( OR , OR )
        {"SQRTV", "(", "OR", ",", "OR", ")"}, // Índice 94: SQRTV ( OR , OR )
        {"STRLEN", "(", "OR", ")"}, // Índice 95: STRLEN ( OR )
        {"concat", "(", "OR", ")"}, // Índice 96: concat ( OR )
        {"copy", "(", "OR", ",", "OR", ")"}, // Índice 97: copy ( OR , OR )
        {"val", "(", "OR", ",", "OR", ",", "OR", ")"}, // Índice 98: val ( OR , OR , OR )
        {"str", "(", "OR", ",", "OR", ")"}, // Índice 99: str ( OR , OR )
        {"sin", "(", "OR", ")"}, // Índice 100: sin ( OR )
        {"cos", "(", "OR", ")"}, // Índice 101: cos ( OR )
        {"tan", "(", "OR", ")"}, // Índice 102: tan ( OR )
        {"chr", "(", "OR", ")"}, // Índice 103: chr ( OR )
        {"pred", "(", "OR", ")"}, // Índice 104: pred ( OR )
        {"succ", "(", "OR", ")"}, // Índice 105: succ ( OR )
        {"inc", "(", "OR", ")"}, // Índice 106: inc ( OR )
        {"dec", "(", "OR", ")"}, // Índice 107: dec ( OR )
        {"sqr", "(", "OR", ")"}, // Índice 108: sqr ( OR )
        {"="}, // Índice 109: =
        {"+="}, // Índice 110: +=
        {"-="}, // Índice 111: -=
        {"/="}, // Índice 112: /=
        {"*="}, // Índice 113: *=
    };

    public final Map<String, Integer> valoresPalabras = new HashMap<>() {{
    put("true", -69);
    put("false", -70);
    put("null", -71);
    put("if", -72);
    put("else", -73);
    put("switch", -74);
    put("for", -75);
    put("do", -76);
    put("while", -77);
    put("Console.log", -78);
    put("forEach", -79);
    put("break", -80);
    put("continue", -81);
    put("let", -82);
    put("const", -83);
    put("undefined", -84);
    put("interface", -85);
    put("typeof", -86);
    put("any", -87);
    put("set", -88);
    put("get", -89);
    put("class", -90);
    put("toLowerCase", -91);
    put("toUpperCase", -92);
    put("length", -93);
    put("trim", -94);
    put("charAt", -95);
    put("startsWith", -96);
    put("endsWith", -97);
    put("indexOf", -98);
    put("Includes", -99);
    put("slice", -100);
    put("replace", -101);
    put("split", -102);
    put("push", -103);
    put("shift", -104);
    put("in", -105);
    put("of", -106);
    put("splice", -107);
    put("concat", -108);
    put("find", -109);
    put("findIndex", -110);
    put("filter", -111);
    put("map", -112);
    put("sort", -113);
    put("reverse", -114);
    put("main",-115);
    put("Console.read",-116);
    put("def",-117);
    put("elseif",-118);
    put("default",-119);
    put("return",-120);
    put("case",-121);
    put("var",-122);
    put("reg",-123);
    put("CLEAR", -124);
    put("SQRT", -125);
    put("POW", -126);
    put("SQRTV", -127);
    put("STRLEN", -128);
    put("copy", -129);
    put("val", -130);
    put("str", -131);
    put("sin", -132);
    put("cos", -133);
    put("tan", -134);
    put("chr", -135);
    put("pred", -136);
    put("succ", -137);
    put("inc", -138);
    put("dec", -139);
    put("sqr", -140);
    
    }};

    public final Map<Integer, String> descripcionErrores = new HashMap<>() {{
        put(500,"Carácter invalido \\n");
        put(501,"Se esperaba [BLO]");
        put(502,"Se esperaba valor binario");
        put(503,"Se esperaba valor octal");
        put(504,"Se esperaba valor hexadecimal");
        put(505,"Se esperaba valor decimal(0-9)");
        put(506,"Se esperaba decimal o [+-]");
        put(507,"Se esperaba [A-Z0-9_]");
        put(508,"Se esperaba [BDOX]");
        put(509,"Valor no reconocido");
        put(510,"Palabra no valida");
        put(511,"Se esperaba una letra");
        put(512,"Se esperaba cierre de comentario (*/)");
        put(513,"Se esperaba cierre de cadena");
        put(514,"Se esperaba var,reg,def,id,main");
        put(515,"Se esperaba var,reg,def,id");
        put(516, "Se esperaba ;");
        put(517, "Se esperaba ,");
        put(518, "Se esperaba reg");
        put(519, "Se esperaba [");
        put(520, "Se esperaba (");
        put(521, "Se esperaba +,-,Const o boleano");
        put(522, "Se esperaba Const o boleano");
        put(523, "Se esperaba Const");
        put(524, "Error STATU");
        put(525, "Se esperaba else o elseif");
        put(526, "Se esperaba , , ; o :");
        put(527, "Se esperaba ; o break");
        put(528, "Se esperaba case o default");
        put(529, "error OR AND");
        put(530, "Se espera | o ||");
        put(531, "Se espera & o &&");
        put(532, "Se esperaba <,>=,<=,!=,==,>,");
        put(533, "Se esperaba <<,>>,>>>");
        put(534, "Se esperaba *,/#,&,%");
        put(535, "Se esperaba ^");
        put(536, "Se esperaba ASIG, [,( ");
        put(537, "Se esperaba ASIG");
        put(538, "Se esperaba ?");
        put(539, "Se esperaba [");
        put(540, "Se esperaba palabra FUNCION");
    }};
    public final Map<String, Integer> noTerminales = new HashMap<>() {{
    put("PROGRAMA", 0);
    put("A1", 1);
    put("A2", 2);
    put("A3", 3);
    put("A4", 4);
    put("A5", 5);
    put("A6", 6);
    put("A7", 7);
    put("A8", 8);
    put("LISTA DE PARAMETROS", 9);
    put("DECLARACION CONSTANTES", 10);
    put("CONSTANTE S/SIGNO", 11);
    put("CONST NUMERICA", 12);
    put("STATU", 13);
    put("B1", 14);
    put("B2", 15);
    put("B3", 16);
    put("B4", 17);
    put("B5", 18);
    put("OR", 19);
    put("C1", 20);
    put("AND", 21);
    put("D1", 22);
    put("EXP_PAS", 23);
    put("E1", 24);
    put("SIMPLE EXP PASCAL", 25);
    put("F1", 26);
    put("TERMINO PASCAL", 27);
    put("G1", 28);
    put("ELEVACION", 29);
    put("H1", 30);
    put("FACTOR", 31);
    put("I1", 32);
    put("I2", 33);
    put("I3", 34);
    put("I4", 35);
    put("ARR", 36);
    put("FUNCION", 37);
    put("ASIG", 38);
}};
    public final Map<String, Integer> Terminales = new HashMap<>() {{
    put("main", 0);
    put("(", 1);
    put(")", 2);
    put("{", 3);
    put("}", 4);
    put("reg", 5);
    put("var", 6);
    put("def", 7);
    put("id", 8);
    put(";", 9);
    put(",", 10);
    put("[", 11);
    put("]", 12);
    put("$", 13);
    put("+", 14);
    put("-", 15);
    put("const real", 16);
    put("const cadena", 17);
    put("binario", 18);
    put("const decimal", 19);
    put("const octal", 20);
    put("const hexadecimal", 21);
    put("true", 22);
    put("false", 23);
    put("const exponencial", 24);
    put("null", 25);
    put("Console.read", 26);
    put("Console.log", 27);
    put("if", 28);
    put("++", 29);
    put("--", 30);
    put("!", 31);
    put("~", 32);
    put("CLEAR", 33);
    put("SQRT", 34);
    put("POW", 35);
    put("SQRTV", 36);
    put("STRLEN", 37);
    put("concat", 38);
    put("copy", 39);
    put("val", 40);
    put("str", 41);
    put("sin", 42);
    put("cos", 43);
    put("tan", 44);
    put("chr", 45);
    put("pred", 46);
    put("succ", 47);
    put("inc", 48);
    put("dec", 49);
    put("sqr", 50);
    put("while", 51);
    put("do", 52);
    put("return", 53);
    put("for", 54);
    put("switch", 55);
    put("elseif", 56);
    put("else", 57);
    put(":", 58);
    put("break", 59);
    put("case", 60);
    put("default", 61);
    put("||", 62);
    put("|", 63);
    put("?", 64);
    put("&&", 65);
    put("&", 66);
    put("<", 67);
    put(">=", 68);
    put("<=", 69);
    put("!=", 70);
    put("==", 71);
    put(">", 72);
    put("<<", 73);
    put(">>", 74);
    put(">>>", 75);
    put("*", 76);
    put("/", 77);
    put("#", 78);
    put("%", 79);
    put("^", 80);
    put("=", 81);
    put("+=", 82);
    put("-=", 83);
    put("/=", 84);
    put("*=", 85);
    put("const ent", 86);
}};
    
    private boolean archivoAbierto      = false;
    private boolean modificado          = false;
    private boolean updatingLineNumbers = false;

    public AnalizadorLexico() {
        super("Analizador Léxico");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 700);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        buildUI();
        setVisible(true);      
    }

    

    public int getColumna(char c) {
    switch (c) {
        case '/':  return 0;
        case '\n': return 2;
        case '=':  return 3;
        case '*':  return 4;
        case '+':  return 5;
        case '-':  return 6;
        case '^':  return 7;
        case '&':  return 8;
        case '|':  return 9;
        case '%':  return 10;
        case '>':  return 11;
        case '<':  return 12;
        case '!':  return 13;
        case '~':  return 14;
        case ',':  return 15;
        case '.':  return 16;
        case ';':  return 17;
        case ':':  return 18;
        case '?':  return 19;
        case '{':  return 20;
        case '}':  return 21;
        case '[':  return 22;
        case ']':  return 23;
        case '(':  return 24;
        case ')':  return 25;
        case '\"': return 26;
        case '\'': return 27;
        case 'X':  return 28;
        case 'x':  return 29;
        case 'B':  return 30;
        case 'b':  return 31;
        case 'O':  return 32;
        case 'o':  return 33;
        case 'L':  return 34;
        case 'l':  return 35;
        case 'D':  return 36;
        
        case 'A','C','E','F','a','c','d','e','f':
            return 37;

        case '0','1':
            return 38;
        case '2','3','4','5','6','7':
            return 39;
        case '8','9':
            return 40;

        case '@':  return 41;
        case 'g', 'G', 'h', 'H', 'i', 'I', 'j', 'J', 'k', 'K', 'm', 'M', 
             'n', 'N','ñ','Ñ', 'p', 'P', 'q', 'Q', 'r', 'R', 's', 'S', 't', 'T', 
             'u', 'U', 'v', 'V', 'w', 'W', 'y', 'Y', 'z', 'Z':
            return 42;
        
        case '_':  return 43;
        case '#':  return 44;
        case '$':  return 45;
        case '¿':  return 46;
        case '¡':  return 47;
        case ' ':  return 48;
        case '\t':  return 49;

        default:
            return 1; 
        }
    }
    public void sumarAGrupo(int estado) {
        switch (estado) {
            case -1,-5,-8,-11,-21,-141:
                categorias[OPERADORES_MATEMATICOS]++;
                break;
            case -2,-4:
                categorias[COMENTARIOS]++;
                break;
            case -3,-7,-10,-13,-15,-18,-22,-23,-26,-30,-35,-37:
                categorias[OPERADORES_ASIGNACION]++;
                break;
            case -6,-9:
                categorias[OPERADORES_POSTFIX]++;
                break;
            case -12:
                categorias[OPERADOR_EXPONENTE]++;
                break;
            case -14,-16,-19,-41:
                categorias[OPERADORES_LOGICOS_BINARIOS]++;
                break;
            case -17,-20,-38:
                categorias[OPERADORES_LOGICOS]++;
                break;
            case -24,-27,-28,-31,-32,-33,-39:
                categorias[OPERADORES_RELACIONALES]++;
                break;
            case -29,-34,-36:
                categorias[OPERADORES_TURNO]++;
                break;
            case -25,-40:
                categorias[OPERADORES_SIN_IGUALDAD]++;
                break;
            case -42,-43,-44,-45:
                categorias[OPERADORES_CONTROL]++;
                break;
            case -46:
                categorias[OPERADOR_TERNARIO]++;
                break;
            case -47,-48,-49,-50,-51,-52:
                categorias[OPERADORES_AGRUPAMIENTO]++;
                break;
            case -53:
                categorias[CONSTANTES_CADENA]++;
                break;
            case -54:
                categorias[CONSTANTES_NUMERICA_BINARIO]++;
                break;
            case -55:
                categorias[CONSTANTES_NUMERICA_OCTAL]++;
                break;
            case -56:
                categorias[CONSTANTES_NUMERICA_HEXADECIMAL]++;
                break;
            case -57:
                categorias[CONSTANTES_NUMERICA_DECIMAL]++;
                break;
            case -58:
                categorias[CONSTANTES_REAL]++;
                break;
            case -59:
                categorias[CONSTANTES_EXPONENCIAL]++;
                break;
            case -60:
                categorias[ID_CADENA]++;
                break;
            case -61:
                categorias[ID_NUMERICA_BINARIO]++;
                break;
            case -62:
                categorias[ID_NUMERICA_DECIMAL]++;
                break; 
            case -63:
                categorias[ID_NUMERICA_OCTAL]++;
                break; 
            case -64:
                categorias[ID_NUMERICA_HEXADECIMAL]++;
                break; 
            case -65:
                categorias[ID_REAL]++;
                break; 
            case -66:
                categorias[ID_EXPONENCIAL]++;
                break;
            case -67:
                categorias[ID_BOOLEANAS]++;
                break;
            case -69,-70:
                categorias[CONSTANTES_BOOLEANAS]++;
                break;
            case -71:
                categorias[CONSTANTES_NULA]++;
                break;
            case -72, -73, -74, -75, -76, -77, -78, -79, -80, -81, -82, -83, -84, -85, -86,
                -87, -88, -89, -90, -91, -92, -93, -94, -95, -96, -97, -98, -99,
                -100, -101, -102, -103, -104, -105, -106, -107, -108, -109, -110, 
                -111, -112, -113, -114, -115, -116, -117, -118, -119, -120, -121, 
                -122, -123, -124, -125, -126, -127, -128, -129, -130, -131, -132, 
                -133, -134, -135, -136, -137, -138, -139, -140:
                categorias[PALABRAS_RESERVADAS]++;
                break;
            case -142:
                categorias[ID_REGISTRO]++;
                break;
            case 500,501,502,503,504,505,506,507,508,509,510,511,512,513:
                categorias[ERRORES]++;
                break;
            default:
                break;
        }
    }
    public int clasificarPalabra(String lexema){
        if (palabrasReservadas.contains(lexema)){
            return valoresPalabras.get(lexema);
        }
        else if (booleanas.contains(lexema)){
            return valoresPalabras.get(lexema);
        }
        else if (nulas.contains(lexema)){
            return valoresPalabras.get(lexema);
        }
        else if(lexema.contains(".")){
            return 510;
        }
        return -142;
    }

    public String getTipoToken(Object[] token){
        int idToken = (int) token[0];
        String lexema = (String) token[1];
        

        if (idToken == -53) return "const cadena";
        if (idToken == -54) return "binario";
        if (idToken == -55) return "const octal";
        if (idToken == -56) return "const hexadecimal";
        if (idToken == -57) return "const decimal";
        if (idToken == -58) return "const real";
        if (idToken == -59) return "const exponencial";
        if (idToken >= -67 && idToken <= -60){
            return "id";
        }
        if (idToken == -142){
            return "id";
        }
        return lexema;
    }

    public static String[][] leerRangoCSV(String archivo, int fIni, int fFin, int cIni, int cFin) throws IOException {
        List<String[]> lineasFiltradas = new ArrayList<>();
        //System.out.println("El programa está buscando en: " + System.getProperty("user.dir"));
        try {
            InputStream is = AnalizadorLexico.class.getClassLoader().getResourceAsStream(archivo);
            //System.out.println(archivo);
            if (is == null) {
                throw new FileNotFoundException("No se encontro la matriz");
            }
            else{
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            String linea;
            int numeroFila = 1;
            while ((linea = br.readLine()) != null) {
                if (numeroFila >= fIni && numeroFila <= fFin) {
                    String[] todasLasColumnas = linea.split(";");
                    int anchoDestino = cFin - cIni + 1;
                    String[] filaRecortada = new String[anchoDestino];
                    for (int j = 0; j < anchoDestino; j++) {
                        int columnaOriginal = cIni - 1 + j;
                        if (columnaOriginal < todasLasColumnas.length) {
                            filaRecortada[j] = todasLasColumnas[columnaOriginal].trim();
                        } else {
                            filaRecortada[j] = "0";
                        }
                    }
                    lineasFiltradas.add(filaRecortada);
                }
                numeroFila++;
            }
            br.close();
            return lineasFiltradas.toArray(new String[0][0]);
            }}catch (Exception e) {
                System.err.println("Error al leer la matriz CSV: " + e.getMessage());
                throw new IOException("No se pudo cargar la matriz de transiciones.");
            }
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        add(buildToolbar(),   BorderLayout.NORTH);
        add(buildMainPanel(), BorderLayout.CENTER);
    }

    // ─── Toolbar ─────────────────────────────────────────────────────────────
    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bar.setBackground(BG_DARK);

        JButton btnAbrir = new JButton("Abrir Archivo");
        btnAbrir.addActionListener(e -> abrirArchivo());
        
        JButton btnCompilar = new JButton("Compilar");
        btnCompilar.addActionListener(e -> compilar());
        
        JButton btnXLS = new JButton("Crear XLS");
        btnXLS.addActionListener(e -> crearXLS());

        // JButton btntxtAvance1 = new JButton("Crear txt Avance 1");
        // btntxtAvance1.addActionListener(e -> ExportarAvance1());
        JButton btntxtAvance2 = new JButton("Crear txt Avance 2");
        btntxtAvance2.addActionListener(e -> ExportarAvance2());
        fileNameLabel = new JLabel(" Sin archivo ");
        fileNameLabel.setForeground(TEXT_MAIN);

        bar.add(btnAbrir);
        bar.add(btnCompilar);
        bar.add(btnXLS);
        //bar.add(btntxtAvance1);
        bar.add(btntxtAvance2);
        bar.add(fileNameLabel);
        
        return bar;
    }

    private JSplitPane buildMainPanel() {
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildCodePanel(), buildRightPanel());
        split.setDividerLocation(450);
        split.setBackground(BG_DARK);
        return split;
    }

    private JPanel buildCodePanel() {
        JPanel p = new JPanel(new BorderLayout());
        
        JLabel lblTitulo = new JLabel(" CÓDIGO FUENTE");
        lblTitulo.setForeground(Color.WHITE);
        p.add(lblTitulo, BorderLayout.NORTH);

        String[] columnas = {"#", "Código"};
        codeTableModel = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 1;
            }
        };

        codeTable = new JTable(codeTableModel);
        codeTable.setFont(new Font("Monospaced", Font.PLAIN, 14));
        codeTable.setBackground(BG_CODE);
        codeTable.setForeground(TEXT_MAIN);
        codeTable.setGridColor(BG_DARK);
        codeTable.setRowHeight(22);
        
        codeTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        TableColumn numColumn = codeTable.getColumnModel().getColumn(0);
        numColumn.setPreferredWidth(50);
        numColumn.setMinWidth(50);
        numColumn.setMaxWidth(50);
        
        numColumn.setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(LINE_NUM_BG);
                c.setForeground(LINE_NUM_FG);
                ((JLabel) c).setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });

        TableColumn codeColumn = codeTable.getColumnModel().getColumn(1);
        codeColumn.setPreferredWidth(1200); // Suficientemente ancho para el scroll horizontal

        codeTableModel.addTableModelListener(e -> {
            if (e.getType() == javax.swing.event.TableModelEvent.UPDATE) {
                onTextChanged();
            }
        });

        JScrollPane scroll = new JScrollPane(codeTable);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.getViewport().setBackground(BG_CODE);
        
        p.add(scroll, BorderLayout.CENTER);

        // Interceptar la tecla ENTER para agregar nuevas filas
        InputMap im = codeTable.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = codeTable.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "addNewRow");
        am.put("addNewRow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (codeTable.isEditing()) {
                    codeTable.getCellEditor().stopCellEditing();
                }
                int currentRow = codeTable.getSelectedRow();
                int rowCount = codeTableModel.getRowCount();

                // Si presionamos Enter en la última fila o en cualquier posición
                // Insertamos una fila vacía justo debajo de la actual
                codeTableModel.insertRow(currentRow + 1, new Object[]{currentRow + 2, ""});
                
                // Seleccionamos la nueva fila y la columna de código
                codeTable.setRowSelectionInterval(currentRow + 1, currentRow + 1);
                codeTable.setColumnSelectionInterval(1, 1);
                
                // Iniciamos el modo edición automáticamente en la nueva fila
                codeTable.editCellAt(currentRow + 1, 1);
                Component editor = codeTable.getEditorComponent();
                if (editor != null) {
                    editor.requestFocus();
                }
                
                // Forzamos la actualización de todos los números de línea
                actualizarNumerosDeLinea();
            }
        });
        //METODO PARA BORRAR lineas
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE,0), "deleteRow");
        am.put("deleteRow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                //System.out.println("ENTRO");
                int row = codeTable.getSelectedRow();
                if (row != -1 && codeTableModel.getRowCount() > 1) {
                    codeTableModel.removeRow(row);
                    actualizarNumerosDeLinea();
                }
            }
        });
        return p;
    }

    private JPanel buildRightPanel() {
        JPanel p = new JPanel(new BorderLayout());
        
        JPanel panelErrores = new JPanel(new BorderLayout());
        JLabel lblErr = new JLabel("ERRORES");
        lblErr.setForeground(Color.BLACK);
        panelErrores.add(lblErr, BorderLayout.NORTH);
        
        String[] colsErr = {"Token Error", "Descripción","Lexema","Tipo de Error", "Línea"};
        errorTableModel = new DefaultTableModel(colsErr, 0);
        JTable tableErrores = new JTable(errorTableModel);
        tableErrores.setBackground(BG_TABLE);
        tableErrores.setForeground(TEXT_MAIN);
        panelErrores.add(new JScrollPane(tableErrores), BorderLayout.CENTER);

        JPanel panelTokens = new JPanel(new BorderLayout());
        JLabel lblTok = new JLabel(" LISTA DE TOKENS");
        lblTok.setForeground(Color.BLACK);
        panelTokens.add(lblTok, BorderLayout.NORTH);
        
        String[] colsTok = {"Token", "Lexema", "Linea"};
        tokenTableModel = new DefaultTableModel(colsTok, 0);
        JTable tableTokens = new JTable(tokenTableModel);
        tableTokens.setBackground(BG_TABLE);
        tableTokens.setForeground(TEXT_MAIN);
        panelTokens.add(new JScrollPane(tableTokens), BorderLayout.CENTER);

        JPanel panelConteo = new JPanel(new BorderLayout());
        JLabel lblCount = new JLabel(" CONTEO POR TIPO");
        lblCount.setForeground(Color.BLACK);
        panelConteo.add(lblCount, BorderLayout.NORTH);
        
        String[] colsCount = {"Tipo de Token", "Cantidad"};

        counterTableModel = new DefaultTableModel(colsCount, 0);
        for (int i = 0; i < nombresGrupos.length; i++) {            
            counterTableModel.addRow(new Object[]{ nombresGrupos[i], categorias[i] });
}
    
        JTable tableConteo = new JTable(counterTableModel);
        tableConteo.setBackground(BG_TABLE);
        tableConteo.setForeground(TEXT_MAIN);
        panelConteo.add(new JScrollPane(tableConteo), BorderLayout.CENTER);

        JSplitPane splitAbajo = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelTokens, panelConteo);
        splitAbajo.setDividerLocation(200);
        
        JSplitPane splitPrincipal = new JSplitPane(JSplitPane.VERTICAL_SPLIT, panelErrores, splitAbajo);
        splitPrincipal.setDividerLocation(150);

        p.add(splitPrincipal, BorderLayout.CENTER);
        return p;
    }
    private void actualizarTablaContadores() {
        for (int i = 0; i < categorias.length; i++) {
            counterTableModel.setValueAt(categorias[i], i, 1);
        }
    }

    private void actualizarListaTokens(){
        for (Object[] token : listaTokens){
            tokenTableModel.addRow(token);
        }
    }

     private void actualizarListaErrores(){
        for (Object[] tokenError : listaErrores){
            errorTableModel.addRow(tokenError);
        }
        for (Object[] tokenErrorSintaxis : listaErroresSintactico) {
            errorTableModel.addRow(tokenErrorSintaxis);
        }
    }


    private void abrirArchivo() {

        JFileChooser fc = new JFileChooser();

        //fc.setDialogTitle("Seleccionar archivo de código");
        // fc.setFileFilter(new FileNameExtensionFilter(
        //     "Archivos de código",
        //     "java", "c", "cpp", "py", "txt", "cs", "js", "ts"));
        fc.setCurrentDirectory(new File(System.getProperty("user.home")));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            cargarArchivo(fc.getSelectedFile());

    }

    private void cargarArchivo(File archivo) {
        try {
            List<String> lineas = new ArrayList<>();
            try (Scanner scanner = new Scanner(archivo, "UTF-8")) {
                while (scanner.hasNextLine()) lineas.add(scanner.nextLine());
            }
            mostrarCodigo(lineas);

            errorTableModel.setRowCount(0);
            tokenTableModel.setRowCount(0);
            java.util.Arrays.fill(categorias, 0);

            archivoAbierto = true;
            modificado = false;
            fileNameLabel.setText(" Archivo: " + archivo.getName() + " ");

        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al leer:\n" + ex.getMessage());
        }
    }

    private void mostrarCodigo(List<String> lineas) {
        updatingLineNumbers = true;
        codeTableModel.setRowCount(0);
        
        for (int i = 0; i < lineas.size(); i++) {
            codeTableModel.addRow(new Object[]{i + 1, lineas.get(i)});
        }
        
        updatingLineNumbers = false;
    }

    private void onTextChanged() {
        if (updatingLineNumbers) return;
        if (archivoAbierto && !modificado) {
            modificado = true;
            fileNameLabel.setText(fileNameLabel.getText().trim() + " editado");
        }
        SwingUtilities.invokeLater(this::actualizarNumerosDeLinea);
    }

    private void actualizarNumerosDeLinea() {
        updatingLineNumbers = true;
        for (int i = 0; i < codeTableModel.getRowCount(); i++) {
            codeTableModel.setValueAt(i + 1, i, 0);
        }
        updatingLineNumbers = false;
    }

    private void compilar() {
        java.util.Arrays.fill(categorias, 0);
        listaErrores.clear();
        listaTokens.clear();
        tokenTableModel.setRowCount(0);
        errorTableModel.setRowCount(0);
        
        StringBuilder sb = new StringBuilder();
        int rowCount = codeTableModel.getRowCount();
        
        for (int r = 0; r < rowCount; r++) {
            Object cellValue = codeTableModel.getValueAt(r, 1);
            if (cellValue != null) {
                sb.append(cellValue.toString());
            }
            if (r < rowCount - 1) {
                sb.append('\n');
            }
        }
        String texto = sb.toString() + " ";
        int estado = 0,columna = 0, nuevoEstado = 0;
        String lexema = "";
        int numeroLinea=1, numeroLineaTemp = 0;
        for (int i = 0; i< texto.length(); i++) {
            char c = texto.charAt(i);
            // System.out.println("LINEA" + i);
            // System.out.println(lexema);
            columna = getColumna(c);
            // System.out.println("estado: " + estado + " Columna: " + columna + " Caracter: '" + c + "'");
            //CASO ESPECIAL CUANDO ES COMENTARIO MULTILINEA PARA GUARDAR LINEA CORRECTA
            if (estado == 1 && columna == 4){
                numeroLineaTemp = numeroLinea;
                // System.out.println("ENTRO A COMENTARIO MULTILINEA, NUMERO LINEA TEMP: " + numeroLineaTemp);
                }
            nuevoEstado = Integer.parseInt(matriz[estado][columna]);
            // System.out.println("Nuevo estado: " + nuevoEstado);
            
            
            if (nuevoEstado < 0){
                if (nuevoEstado == -68){
                    int tokenPalabra = clasificarPalabra(lexema);

                    if (tokenPalabra == 510){
                        nuevoEstado = tokenPalabra;
                        sumarAGrupo(nuevoEstado);
                        Object[] datosError = {nuevoEstado,descripcionErrores.get(nuevoEstado),lexema,"Lexico",numeroLinea};
                        listaErrores.add(datosError);
                        // System.out.println("token erroneo:'" + lexema+"'");
                    }
                    else{
                        nuevoEstado = tokenPalabra;
                    }
                }

                if (nuevoEstado != 510){
                    sumarAGrupo(nuevoEstado);
                if (nuevoEstado != -2 && nuevoEstado != -4){
                    Object[] datosToken = {nuevoEstado,lexema,numeroLinea};
                    listaTokens.add(datosToken);
                    // System.out.println("token agregado : " + lexema);
                }
                // System.out.println("token reconocido:'" + lexema+"'");
                // System.out.println("linea: " + (nuevoEstado == -4 ? numeroLineaTemp : numeroLinea) );
                
                }
                numeroLineaTemp = 0;
                estado = 0;
                nuevoEstado = 0;
                lexema = "";
                i--;

            }
            else if (nuevoEstado >= 500){
                sumarAGrupo(nuevoEstado);
                lexema += c;
                // System.out.println("token erroneo:'" + lexema+"'");
                // System.out.println("linea: " + numeroLinea );
                Object[] datosError = {nuevoEstado,descripcionErrores.get(nuevoEstado),lexema,"Lexico",numeroLinea};
                listaErrores.add(datosError);
                if (estado != 0){
                    i--;
                }
                estado = 0;
                nuevoEstado = 0;
                lexema = "";   
                
                
            }
            else{
                estado = nuevoEstado;
                if (nuevoEstado != 0){
                    lexema += c;
                }
                if (c == '\n') {
                numeroLinea++;
            }
            }
            
        }
        if (nuevoEstado == 2){
            sumarAGrupo(-2);
        }
        else if (nuevoEstado == 55 || nuevoEstado == 57){
            sumarAGrupo(513);
            Object[] datosError = {513,descripcionErrores.get(513),lexema,"Lexico",numeroLinea};
            listaErrores.add(datosError);
            
        }
        else if (nuevoEstado == 4 || nuevoEstado == 5){
            sumarAGrupo(512);
            Object[] datosError = {512,descripcionErrores.get(512),lexema,"Lexico",numeroLineaTemp};
            listaErrores.add(datosError);
        }
        actualizarTablaContadores();
        actualizarListaTokens();
        
        //Fase 2: Analizador sintactico
        analizarSintactico();
    }

     private void analizarSintactico(){
        pilaSintactica.clear();
        contadorNoTerminales.clear();
        listaErroresSintactico.clear();
        ListaCambiosDeclaracion.clear();
        pilaAmbito.clear();
        ListaCambiosAmbito.clear();
        pilaAmbito.push(0);
        int lineaFinal = 0;
        int numAmbito = 1;
        if (!listaTokens.isEmpty() && listaTokens.get(listaTokens.size()-1)[1].equals("$")) {
            listaTokens.remove(listaTokens.size()-1);
        }
        //System.out.println("--------------------START SINTAX ------------------");
        pilaSintactica.push("$");
        pilaSintactica.push("PROGRAMA");

        int i = 0;
        Object[] tokenFin = {0, "$", 0};
        listaTokens.add(tokenFin);
        // for (Object[] token : listaTokens) {
        //     System.out.println("token -> " + token[0] + 
        //                " | Lexema: [" + token[1] + 
        //                "] | Línea: " + token[2] + 
        //                " CATEGORIA : " + token[1]);
        // }
        while(!pilaSintactica.isEmpty() && i < listaTokens.size() ){
            String tope = pilaSintactica.peek();
            Object[] tokenActual = listaTokens.get(i);
            String terminalActual = getTipoToken(tokenActual);
            //System.out.println("Pila Sintactica : " + pilaSintactica);
            //System.out.println("tope: "+tope + " , lexema actual : "+ terminalActual);

            if (tope.equals("800")){
                EsDeclaracion = false;
                Object[] nuevaLinea = {tokenActual[2], "Declaracion = falso" };
                Object[] nuevaLinea2 = {tokenActual[2], "Ejecucion = true" };
                ListaCambiosDeclaracion.add(nuevaLinea);
                ListaCambiosDeclaracion.add(nuevaLinea2);
                //System.out.println("entrando a ejecucion");
                pilaSintactica.pop();
                continue;
            }

            if (tope.equals("801")){
                EsDeclaracion = true;
                Object[] nuevaLinea2 = {tokenActual[2], "Ejecucion = false" };
                Object[] nuevaLinea = {tokenActual[2], "Declaracion = true" };
                ListaCambiosDeclaracion.add(nuevaLinea2);
                ListaCambiosDeclaracion.add(nuevaLinea);
                //System.out.println("Volviendo a declaracion");
                pilaSintactica.pop();
                continue;
            }

            if (tope.equals("802")){
                pilaAmbito.push(numAmbito);
                Object[] nuevaLinea = {tokenActual[2],numAmbito,"Creo"};
                ListaCambiosAmbito.add(nuevaLinea);
                numAmbito++;
                pilaSintactica.pop();
                continue;
            }

            if (tope.equals("803")){
                int eliminado = pilaAmbito.pop();
                Object[] nuevaLinea = {tokenActual[2],eliminado,"Exit"};
                ListaCambiosAmbito.add(nuevaLinea);
                pilaSintactica.pop();
                continue;
            }
            if (i == listaTokens.size() - 2){
                lineaFinal = Integer.valueOf( String.valueOf(tokenActual[2]));
            }

            if (tope.equals(terminalActual)){
                pilaSintactica.pop();
                i++;
                //System.out.println("Match: " + terminalActual);
            }
            else if (noTerminales.containsKey(tope)){
                contadorNoTerminales.put(tope, contadorNoTerminales.getOrDefault(tope, 0) + 1);

                Integer fila = noTerminales.get(tope);
                Integer columna = Terminales.get(terminalActual);

                if (columna == null) {
                    System.err.println("Error Sintáctico: Token '" + terminalActual + "' no reconocido en la tabla.");
                    //TODO Mandarlo a lista aunque no deberia ocurrir en pruebas
                    break;
                }
                int indiceProduccion = Integer.parseInt(matrizSintactica[fila][columna]);

                if (indiceProduccion >= 500) {
                    //System.err.println("Error Sintáctico en línea " + tokenActual[2] + 
                    //                 ": No se esperaba '" + terminalActual + "'");
                    //MANDAR ERROR A LISTA
                    Object[] datosError = {indiceProduccion,descripcionErrores.get(indiceProduccion),tokenActual[1],"Sintactico",tokenActual[2]};
                    listaErroresSintactico.add(datosError);
                    //categorias[ERRORES]++;

                    i++;
                    if (i >= listaTokens.size()) {
                    break; // Protección contra crasheo si el error es al final
                }
                    continue;
                }
                if (indiceProduccion == 13){
                    //System.out.println("Aplicando producción epsilon " + indiceProduccion + " para " + tope);
                    pilaSintactica.pop();
                    continue;
                }
                pilaSintactica.pop();
                String[] simbolosProduccion = producciones[indiceProduccion];
                // Empilar al revés, ignorando el símbolo vacío o épsilon
                for (int k = simbolosProduccion.length - 1; k >= 0; k--) {
                    String simbolo = simbolosProduccion[k];
                    if (!simbolo.equals("ε") && !simbolo.isEmpty()) {
                        pilaSintactica.push(simbolo);
                    }
                }
                //System.out.println("Aplicando producción " + indiceProduccion + " para " + tope+" con "+terminalActual);

            }
            else{
                String descripcion = "El tope de la pila '" + tope + "' no coincide con el lexema '" + terminalActual + "'";
                System.err.println("Error Sintáctico: El tope de la pila '" + tope + 
                               "' no coincide con '" + terminalActual + "'");
                Object[] datosError = {541, descripcion, tokenActual[1], "Sintactico", tokenActual[2]};
                listaErroresSintactico.add(datosError);
                break;
            }
        }
        if (!pilaAmbito.isEmpty()){
            int eliminado = pilaAmbito.pop();
            Object[] nuevaLinea = {lineaFinal,eliminado,"Exit"};
            ListaCambiosAmbito.add(nuevaLinea);
        }
        if (pilaSintactica.isEmpty()) {
            System.out.println("¡Análisis sintáctico exitoso!");
        } else {
            System.out.println("El análisis terminó con errores (Pila no vacía).");
        }
        actualizarListaErrores();
        imprimirEstadisticasNoTerminales();
        // for (Object[] elemento : ListaCambiosAmbito) {
        //     System.out.println("linea: " + elemento[0] + " ambito: " + elemento[1] + " ocurrio = " + elemento[2]);
        // }
     }

     private void imprimirEstadisticasNoTerminales() {
        //System.out.println("\n--- ESTADÍSTICAS DE NO TERMINALES ---");
        // Ordenar y mostrar resultados
        // 1. Mostrar Lista de Errores si existen
        if (!listaErroresSintactico.isEmpty()) {
            //System.out.println("\n--- LISTA DE ERRORES ENCONTRADOS ---");
            listaErroresSintactico.forEach(error -> {
                // Estructura: {Código, Descripción, Lexema, Tipo, Línea}
                // System.out.printf("Error [%s]: %s | Lexema: '%s' | Línea: %s%n", 
                //     error[0], error[1], error[2], error[4]);
            });
        } else {
            //System.out.println("\nNo se encontraron errores sintácticos.");
        }
        contadorNoTerminales.forEach((nombre, cantidad) -> {
            //System.out.println("No Terminal [" + nombre + "]: " + cantidad + " veces.");
        });
    }
    


    private void crearXLS() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar Resultados a Excel");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivo de Excel (*.xlsx)", "xlsx"));

        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                File archivo = fc.getSelectedFile();
                if (!archivo.getName().toLowerCase().endsWith(".xlsx")) {
                    archivo = new File(archivo.getParentFile(), archivo.getName() + ".xlsx");
                    }

        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle estiloCabecera = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font fuenteCabecera = workbook.createFont();
            fuenteCabecera.setBold(true);
            estiloCabecera.setFont(fuenteCabecera);

            //TOKENS VÁLIDOS
            Sheet hojaTokens = workbook.createSheet("TOKENS");
            String[] cabecera1 = {"Estado", "Lexema", "Linea"};
            crearFilaCabecera(hojaTokens, cabecera1, estiloCabecera);
            llenarDatosHoja(hojaTokens, listaTokens);

            //ERRORES
            Sheet hojaErrores = workbook.createSheet("ERRORES");
            String[] cabecera2 = {"Token", "Descripcion", "Lexema", "Tipo de error", "Linea"};
            crearFilaCabecera(hojaErrores, cabecera2, estiloCabecera);
        
            List<Object[]> erroresUnificados = new ArrayList<>();

            if (listaErrores != null) {
                erroresUnificados.addAll(listaErrores);
            }
            if (listaErroresSintactico != null) {
                erroresUnificados.addAll(listaErroresSintactico);
            }
            // Mandamos la lista unificada a la hoja
            llenarDatosHoja(hojaErrores, erroresUnificados);

            //CONTADORES
            Sheet hojaCategorias = workbook.createSheet("CONTADORES");

            // 4. NUEVA HOJA: SINTAXIS (Basada en la plantilla Hoja de Sintaxis.xlsx)
            Sheet hojaSintaxis = workbook.createSheet("SINTAXIS");
            String[] cabeceraSintaxis = {
                "Errores", "PROGRAMA", "LISTA DE PARAMETROS", "EXP PAS", 
                "CONSTANTE S/SIGNO", "CONST NUMÉRICA", "OR", "AND", 
                "DECLARACION CONSTANTES", "FACTOR", "ELEVACION", 
                "TERMINO PASCAL", "Simple Exp Pascal", "STATU", "Funcion", "ASIG", "ARR"
            };

            Row filaCabSintaxis = hojaSintaxis.createRow(0);
            for (int i = 0; i < cabeceraSintaxis.length; i++) {
                Cell celda = filaCabSintaxis.createCell(i);
                celda.setCellValue(cabeceraSintaxis[i]);
                celda.setCellStyle(estiloCabecera);
                hojaSintaxis.setColumnWidth(i, 20 * 256);
            }

            // Fila de datos de Sintaxis
            Row filaDatosSintaxis = hojaSintaxis.createRow(1);
            
            // Celda 0: Total de errores sintácticos
            filaDatosSintaxis.createCell(0).setCellValue(listaErroresSintactico.size());

            // Mapeo de contadores (Usando los nombres exactos de tu contadorNoTerminales)
            // Se usa getOrDefault para poner 0 si el No Terminal no fue llamado
            filaDatosSintaxis.createCell(1).setCellValue(contadorNoTerminales.getOrDefault("PROGRAMA", 0));
            filaDatosSintaxis.createCell(2).setCellValue(contadorNoTerminales.getOrDefault("LISTA DE PARAMETROS", 0));
            filaDatosSintaxis.createCell(3).setCellValue(contadorNoTerminales.getOrDefault("EXP_PAS", 0));
            filaDatosSintaxis.createCell(4).setCellValue(contadorNoTerminales.getOrDefault("CONSTANTE S/SIGNO", 0));
            filaDatosSintaxis.createCell(5).setCellValue(contadorNoTerminales.getOrDefault("CONST NUMERICA", 0));
            filaDatosSintaxis.createCell(6).setCellValue(contadorNoTerminales.getOrDefault("OR", 0));
            filaDatosSintaxis.createCell(7).setCellValue(contadorNoTerminales.getOrDefault("AND", 0));
            filaDatosSintaxis.createCell(8).setCellValue(contadorNoTerminales.getOrDefault("DECLARACION CONSTANTES", 0));
            filaDatosSintaxis.createCell(9).setCellValue(contadorNoTerminales.getOrDefault("FACTOR", 0));
            filaDatosSintaxis.createCell(10).setCellValue(contadorNoTerminales.getOrDefault("ELEVACION", 0));
            filaDatosSintaxis.createCell(11).setCellValue(contadorNoTerminales.getOrDefault("TERMINO PASCAL", 0));
            filaDatosSintaxis.createCell(12).setCellValue(contadorNoTerminales.getOrDefault("SIMPLE EXP PASCAL", 0));
            filaDatosSintaxis.createCell(13).setCellValue(contadorNoTerminales.getOrDefault("STATU", 0));
            filaDatosSintaxis.createCell(14).setCellValue(contadorNoTerminales.getOrDefault("FUNCION", 0));
            filaDatosSintaxis.createCell(15).setCellValue(contadorNoTerminales.getOrDefault("ASIG", 0));
            filaDatosSintaxis.createCell(16).setCellValue(contadorNoTerminales.getOrDefault("ARR", 0));

            CellStyle estiloWrap = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font fuenteCabecera2 = workbook.createFont();
            estiloWrap.setFont(fuenteCabecera2);
            estiloWrap.setWrapText(true);
            estiloWrap.setAlignment(HorizontalAlignment.CENTER);
            estiloWrap.setVerticalAlignment(VerticalAlignment.CENTER);
            estiloWrap.setBorderTop(BorderStyle.THIN);
            estiloWrap.setBorderBottom(BorderStyle.THIN);
            estiloWrap.setBorderLeft(BorderStyle.THIN);
            estiloWrap.setBorderRight(BorderStyle.THIN);

            // 2. Fila 1: Nombres de los grupos
            Row filaNombres = hojaCategorias.createRow(0);
            filaNombres.setHeightInPoints(40); // <--- Aumentamos el alto (30-40 es ideal para 2 líneas)

            for (int i = 0; i < nombresGrupos.length; i++) {
                Cell celda = filaNombres.createCell(i);
                celda.setCellValue(nombresGrupos[i]);
                celda.setCellStyle(estiloWrap);
            
                hojaCategorias.setColumnWidth(i, 15 * 256); 
            }

            Row filaConteos = hojaCategorias.createRow(1);
            CellStyle estiloConteos = workbook.createCellStyle();
            estiloConteos.setAlignment(HorizontalAlignment.CENTER);
            estiloConteos.setBorderTop(BorderStyle.THIN);
            estiloConteos.setBorderBottom(BorderStyle.THIN);
            estiloConteos.setBorderLeft(BorderStyle.THIN);
            estiloConteos.setBorderRight(BorderStyle.THIN);

            for (int i = 0; i < categorias.length; i++) {
                Cell celda = filaConteos.createCell(i);
                celda.setCellValue(categorias[i]);
                celda.setCellStyle(estiloConteos);
            }

            try (FileOutputStream out = new FileOutputStream(archivo)) {
                workbook.write(out);
            }
            JOptionPane.showMessageDialog(this, "Excel creado exitosamente.");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al crear Excel: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

private void crearFilaCabecera(Sheet hoja, String[] columnas, CellStyle estilo) {
    Row fila = hoja.createRow(0);
    for (int i = 0; i < columnas.length; i++) {
        Cell celda = fila.createCell(i);
        celda.setCellValue(columnas[i]);
        celda.setCellStyle(estilo);
    }
}

private void llenarDatosHoja(Sheet hoja, List<Object[]> datos) {
    for (int i = 0; i < datos.size(); i++) {
        Row fila = hoja.createRow(i + 1);
        Object[] info = datos.get(i);
        for (int j = 0; j < info.length; j++) {
            Cell celda = fila.createCell(j);
            if (info[j] instanceof Number) {
                celda.setCellValue(((Number) info[j]).doubleValue());
            } else {
                celda.setCellValue(info[j].toString());
            }
        }
    }
    for (int i = 0; i < hoja.getRow(0).getPhysicalNumberOfCells(); i++) {
        hoja.autoSizeColumn(i);
    }
}
    //Ambito
    private void ExportarAvance1() {
        // Configuramos el JFileChooser
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Exportar Avance a TXT");
        
        // Filtro para que por defecto busque/guarde con extensión .txt
        fc.setFileFilter(new FileNameExtensionFilter("Archivo de Texto (*.txt)", "txt"));

        // Mostrar la ventana de diálogo para guardar. 
        // 'this' asume que el método está dentro de un JFrame o JPanel.
        if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File archivo = fc.getSelectedFile();
            
            // Asegurarnos de que el archivo termine con la extensión .txt
            if (!archivo.getName().toLowerCase().endsWith(".txt")) {
                archivo = new File(archivo.getParentFile(), archivo.getName() + ".txt");
            }

            // Procedemos a escribir el archivo en la ruta seleccionada
            try (FileWriter fw = new FileWriter(archivo);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter out = new PrintWriter(bw)) {

                out.println("Linea: 1 | Ocurrió: Declaracion = true ");

                for (Object[] elemento : ListaCambiosDeclaracion) {
                    String lineaExportar = "Línea: " + elemento[0] + " | Ocurrió: " + elemento[1];
                    out.println(lineaExportar);
                }

                // Opcional: Mostrar un mensaje de éxito al usuario
                JOptionPane.showMessageDialog(this, 
                    "El avance se ha exportado correctamente a:\n" + archivo.getAbsolutePath(), 
                    "Exportación Exitosa", 
                    JOptionPane.INFORMATION_MESSAGE);
                
                System.out.println("El avance se ha exportado en: " + archivo.getAbsolutePath());

            } catch (IOException e) {
                // Mostrar un mensaje de error al usuario
                JOptionPane.showMessageDialog(this, 
                    "Error al guardar el archivo:\n" + e.getMessage(), 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                
                System.err.println("Error al intentar exportar el archivo: " + e.getMessage());
            }
        }
    }
    private void ExportarAvance2() {
    // Configuramos el JFileChooser
    JFileChooser fc = new JFileChooser();
    fc.setDialogTitle("Exportar Avance a TXT");
    
    // Filtro para que por defecto busque/guarde con extensión .txt
    fc.setFileFilter(new FileNameExtensionFilter("Archivo de Texto (*.txt)", "txt"));

    // ASIGNAR NOMBRE POR DEFECTO AQUÍ
    fc.setSelectedFile(new File("Ambito-BrandonFornes2.txt")); 

    // Mostrar la ventana de diálogo para guardar. 
    // 'this' asume que el método está dentro de un JFrame o JPanel.
    if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        File archivo = fc.getSelectedFile();
        
        // Asegurarnos de que el archivo termine con la extensión .txt
        if (!archivo.getName().toLowerCase().endsWith(".txt")) {
            archivo = new File(archivo.getParentFile(), archivo.getName() + ".txt");
        }

        // Procedemos a escribir el archivo en la ruta seleccionada
        try (FileWriter fw = new FileWriter(archivo);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {

            out.println("Linea: 1 | Ambito: 0 -> Creo");

            for (Object[] elemento : ListaCambiosAmbito) {
                String lineaExportar = "Línea: " + elemento[0] + " | Ambito: " + elemento[1] + " " + elemento[2];
                out.println(lineaExportar);
            }

            // Opcional: Mostrar un mensaje de éxito al usuario
            JOptionPane.showMessageDialog(this, 
                "El avance se ha exportado correctamente a:\n" + archivo.getAbsolutePath(), 
                "Exportación Exitosa", 
                JOptionPane.INFORMATION_MESSAGE);
            
            System.out.println("El avance se ha exportado en: " + archivo.getAbsolutePath());

        } catch (IOException e) {
            // Mostrar un mensaje de error al usuario
            JOptionPane.showMessageDialog(this, 
                "Error al guardar el archivo:\n" + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            
            System.err.println("Error al intentar exportar el archivo: " + e.getMessage());
        }
    }
}

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        try {
            matriz = leerRangoCSV("MATRIZ_CSV.csv", 2, 93, 2, 51);
            //System.out.println("Primer valor cargado: " + matriz[0][1]);
            //System.out.println(matriz[91][28]);
            matrizSintactica = leerRangoCSV("TABLA_SINTACTICO.csv", 2, 40, 2, 88);
            //System.out.println("primer valor en sintactico: " + matrizSintactica[38][86]);
            //System.out.println(java.util.Arrays.toString(producciones[50]));
        } catch (IOException e) {
            System.err.println("Error: No se encontró el archivo matriz.");
        }
        SwingUtilities.invokeLater(AnalizadorLexico::new);
    }
}