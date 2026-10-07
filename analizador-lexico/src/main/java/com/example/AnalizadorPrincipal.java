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

public class AnalizadorPrincipal extends JFrame { 

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
    private ArrayList<Object[]> listaErroresSemantico = new ArrayList<>();
    private ArrayList<Object[]> listaErroresSemantico1 = new ArrayList<>();
    // Variables ambito
    private ArrayList<Object[]> ListaCambiosDeclaracion = new ArrayList<>();
    private ArrayList<Object[]> ListaCambiosAmbito = new ArrayList<>();
    private Stack<Integer> pilaContadorAmbito = new Stack<>();
    private Deque<Map<String, Identificador>> pilaIdentificadores = new ArrayDeque<>();;
    private List<Identificador> registroIdentificadores = new ArrayList<>();
    private boolean EsDeclaracion = true;

    private int indiceGlobalPrefijo = 0;
    
    //Variables SEMANTICA
    boolean leyendoOperacion = false;
    List<Object[]> expresionActual = new ArrayList<>();
    int lineaOperacionActual = 0;
    public List<String> registroOperaciones = new ArrayList<>();
    public List<List<Object[]>> operacionesPrefijasTokens = new ArrayList<>();
    public List<Object[]> listaResumenOperaciones = new ArrayList<>();

    Object[] tokenIzquierdo = null;
    Object[] tokenAsignacion = null;

    private List<String[]> listaCuadruplos = new ArrayList<>();

    private int[] contadoresTemporales = new int[9];

    public static final int BIN = 0;
    public static final int DEC = 1;
    public static final int OCT = 2;
    public static final int HEX = 3;
    public static final int REAL = 4;
    public static final int EXP = 5;
    public static final int CAD = 6;
    public static final int BOOL = 7;
    public static final int VAR = 8;
    private static final int E = -1;

    private static final String[] NOMBRES = {
        "Binario", "Decimal", "Octal", "Hexadecimal", "Real", "Exponencial", "Cadena", "Booleana", "Variant"
    };
    private static final String[] PREFIJOS_TEMP = {
        "TBI", "TD", "TO", "TH", "TR", "TE", "TC", "TB", "TV"
    };

     private static final int[][] SUMA = {
        { BIN, E, E, E, E, E, CAD, E, BIN },
        { E, DEC, E, E, REAL, EXP, CAD, E, DEC },
        { E, E, OCT, E, E, E, CAD, E, OCT },
        { E, E, E, HEX, E, E, CAD, E, HEX },
        { E, REAL, E, E, REAL, EXP, CAD, E, REAL },
        { E, EXP, E, E, EXP, EXP, CAD, E, EXP },
        { CAD, CAD, CAD, CAD, CAD, CAD, CAD, CAD, CAD },
        { E, E, E, E, E, E, CAD, E, E },
        { BIN, DEC, OCT, HEX, REAL, EXP, CAD, E, VAR }
    };
    private static final int[][] RESTA = {
        { BIN, E, E, E, E, E, E, E, BIN },
        { E, DEC, E, E, REAL, EXP, E, E, DEC },
        { E, E, OCT, E, E, E, E, E, OCT },
        { E, E, E, HEX, E, E, E, E, HEX },
        { E, REAL, E, E, REAL, EXP, E, E, REAL },
        { E, EXP, E, E, EXP, EXP, E, E, EXP },
        { E, E, E, E, E, E, E, E, E },
        { E, E, E, E, E, E, E, E, E },
        { BIN, DEC, OCT, HEX, REAL, EXP, E, E, VAR }
    };
    private static final int[][] MULT = RESTA;
    private static final int[][] DIV = {
        // BIN, DEC,  OCT,  HEX,  REAL, EXP,  CAD, BOOL, VAR
        { BIN,  E,    E,    E,    E,    E,    E,   E,    BIN  }, // Bin
        { E,    REAL, E,    E,    REAL, EXP,  E,   E,    REAL }, // Dec
        { E,    E,    OCT,  E,    E,    E,    E,   E,    OCT  }, // Oct
        { E,    E,    E,    HEX,  E,    E,    E,   E,    HEX  }, // Hex
        { E,    REAL, E,    E,    EXP,  EXP,  E,   E,    EXP  }, // Real
        { E,    EXP,  E,    E,    EXP,  EXP,  E,   E,    EXP  }, // Exp
        { E,    E,    E,    E,    E,    E,    E,   E,    E    }, // Cadena
        { E,    E,    E,    E,    E,    E,    E,   E,    E    }, // Boolean
        { BIN,  REAL, OCT,  HEX,  EXP,  EXP,  E,   E,    VAR  }  // Variant
    };
    private static final int[][] REL = {
        // BIN, DEC,  OCT,  HEX,  REAL, EXP,  CAD, BOOL, VAR
        { BOOL, E,    E,    E,    E,    E,    E,   E,    BOOL }, // Bin
        { E,    BOOL, E,    E,    BOOL, BOOL, E,   E,    BOOL }, // Dec
        { E,    E,    BOOL, E,    E,    E,    E,   E,    BOOL }, // Oct
        { E,    E,    E,    BOOL, E,    E,    E,   E,    BOOL }, // Hex
        { E,    BOOL, E,    E,    BOOL, BOOL, E,   E,    BOOL }, // Real
        { E,    BOOL, E,    E,    BOOL, BOOL, E,   E,    BOOL }, // Exp
        { E,    E,    E,    E,    E,    E,    E,   E,    BOOL }, // Cadena
        { E,    E,    E,    E,    E,    E,    E,   E,    E    }, // Boolean
        { BOOL, BOOL, BOOL, BOOL, BOOL, BOOL, BOOL,E,    BOOL }  // Variant
    };
    private static final int[][] LOG = {
        { E, E, E, E, E, E, E, E, E },
        { E, E, E, E, E, E, E, E, E },
        { E, E, E, E, E, E, E, E, E },
        { E, E, E, E, E, E, E, E, E },
        { E, E, E, E, E, E, E, E, E },
        { E, E, E, E, E, E, E, E, E },
        { E, E, E, E, E, E, E, E, E },
        { E, E, E, E, E, E, E, BOOL, BOOL },
        { E, E, E, E, E, E, E, BOOL, VAR }
    };

     private static final int[][] POT = {
        { E, BIN, E, E, E, E, E, E, BIN },
        { E, DEC, E, E, E, E, E, E, REAL },
        { E, OCT, E, E, E, E, E, E, OCT },
        { E, HEX, E, E, E, E, E, E, HEX },
        { E, REAL, E, E, E, E, E, E, EXP },
        { E, EXP, E, E, E, E, E, E, EXP },
        { E, E, E, E, E, E, E, E, E },
        { E, E, E, E, E, E, E, E, E },
        { E, VAR, E, E, E, E, E, E, VAR }
    };


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
        {"reg","813", "id", "{","802","806", "id", "A3", "}","803","810", "A1"},
        {",", "id", "A3"},
        {"var","804", "A4", "id", "A6", "A7", ";","810", "A1"},
        {"reg","814", "id","815"},
        {",","807", "const decimal", "A5"},
        {"[","807", "const decimal", "A5", "]"},
        {",", "A4", "id", "A6", "A7"},
        {"def","805", "id","802" ,"LISTA DE PARAMETROS", "PROGRAMA","803", ";","810", "A1"},
        {"808","id", "=", "DECLARACION CONSTANTES", "A8", ";","810", "A1"},
        {",", "id", "=", "DECLARACION CONSTANTES", "A8"}, 
        {"ε"},
        {"(","806", "id", "A3", ")","810"},
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
        {"for", "(","802","801", "OR", "B3", ")", "STATU","803"},
        {",", "OR", "B3"},
        {":","800", "OR"},
        {";","800", "STATU", ";", "OR", "B1"},
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
        {"ASIG","811", "OR","812", "I3"}, // Índice 79: ASIG OR I3
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

    public AnalizadorPrincipal() {
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
        //System.out.println("lexema es 1 " + lexema + " y token " + idToken);

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
            //System.out.println("lexema es " + lexema);
            return "id";
        }
        return lexema;
    }

    public String getTipo(Object[] token){
        int numToken = Integer.parseInt(token[0].toString());
        switch (numToken) {
            case -53,-60:
                return "Cadena";
            case -54,-61:
                return "Bin";
            case -57,-62:
                return "Dec";
            case -55,-63:
                return "Oct";
            case -56,-64:
                return "Hex";
            case -58,-65:
                return "Real";
            case -59,-66:
                return "exp";
            case -67:
                return "Boolean";
        
            default:
                break;
        }
        return "";
    }

    public static String[][] leerRangoCSV(String archivo, int fIni, int fFin, int cIni, int cFin) throws IOException {
        List<String[]> lineasFiltradas = new ArrayList<>();
        //System.out.println("El programa está buscando en: " + System.getProperty("user.dir"));
        try {
            InputStream is = AnalizadorPrincipal.class.getClassLoader().getResourceAsStream(archivo);
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
        //JButton btntxtAvance2 = new JButton("Crear txt Avance 2");
        //btntxtAvance2.addActionListener(e -> ExportarAvance2());

        // JButton btntxtAvance1Semantica = new JButton("Crear txt Avance 1 Semantica");
        // btntxtAvance1Semantica.addActionListener(e -> ExportarOperacionesTXT());

        // JButton btntxtAvance2Semantica = new JButton("Crear txt Avance 2 Semantica");
        // btntxtAvance2Semantica.addActionListener(e -> ExportarCuadruplosTXT());

        fileNameLabel = new JLabel(" Sin archivo ");
        fileNameLabel.setForeground(TEXT_MAIN);

        bar.add(btnAbrir);
        bar.add(btnCompilar);
        bar.add(btnXLS);
        //bar.add(btntxtAvance1);
        //bar.add(btntxtAvance2);
        //bar.add(btntxtAvance1Semantica);
        // bar.add(btntxtAvance2Semantica);
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
        for (Object[] tokenErrorSemantico : listaErroresSemantico) {
            errorTableModel.addRow(tokenErrorSemantico);
        }
        for (Object[] tokenErrorSemantico1 : listaErroresSemantico1) {
            errorTableModel.addRow(tokenErrorSemantico1);
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
        listaErroresSemantico.clear();
        listaErroresSemantico1.clear();
        //AMBITO
        ListaCambiosDeclaracion.clear();
        pilaContadorAmbito.clear();
        ListaCambiosAmbito.clear();
        pilaContadorAmbito.push(0);
        String claseActual = ""; // Puede ser "VAR", "FUNC", "PAR", o ""
        int numAmbito = 1;
        Identificador funcionActual = null;
        Identificador arregloActual = null;
        boolean esTipoRegistro = false;
        boolean esDefinicionRegistro = false;
        String tipoPersonalizado = "";
        pilaIdentificadores.clear();
        pilaIdentificadores.push(new HashMap<>());
        registroIdentificadores.clear();
        registroOperaciones.clear();
        operacionesPrefijasTokens.clear();
        listaResumenOperaciones.clear();
        contadoresTemporales = new int[9];
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
                claseActual = "";
                pilaSintactica.pop();
                continue;
            }

            if (tope.equals("801")){
                EsDeclaracion = true;
                claseActual = "VAR";
                pilaSintactica.pop();
                continue;
            }

            if (tope.equals("802")){
                pilaIdentificadores.push(new HashMap<>());
                pilaContadorAmbito.push(numAmbito);
                numAmbito++;
                pilaSintactica.pop();
                continue;
            }

            if (tope.equals("803")){
                if (!pilaIdentificadores.isEmpty()) {
                pilaIdentificadores.pop();
                }
                pilaContadorAmbito.pop();
                pilaSintactica.pop();
                continue;
            }
            if (tope.equals("804")){
                claseActual = "VAR";
                pilaSintactica.pop();
                continue;
            }
            if (tope.equals("805")){
                claseActual = "FUNC";
                pilaSintactica.pop();
                continue;
            }
            if (tope.equals("806")){
                claseActual = "PAR";
                pilaSintactica.pop();
                continue;
            }
            if (tope.equals("808")){
                claseActual = "CONST";
                pilaSintactica.pop();
                continue;
            }
            if (tope.equals("813")){
                claseActual = "REG";
                esDefinicionRegistro = true; // ¡NUEVO! Es el padre, debe llevar contadores
                pilaSintactica.pop();
                continue;
            }
            if (tope.equals("814")){
                esTipoRegistro = true; // Avisamos que el próximo ID no es variable, es un Tipo
                pilaSintactica.pop();
                continue;
            }
            if (tope.equals("815")){
                claseActual = "REG";
                esDefinicionRegistro = false; // ¡NUEVO! Es solo una variable, NO lleva contadores
                pilaSintactica.pop();
                continue;
            }
            
            if (tope.equals("810")){
                claseActual = ""; // Regresa a modo "Uso de variable" (no declaración)
                tipoPersonalizado = "";
                esDefinicionRegistro = false; // ¡NUEVO! Apagamos la bandera por seguridad
                arregloActual = null;   // ¡Seguridad añadida! Matamos la referencia al arreglo
                funcionActual = null;   // También limpiamos la función por si acaso
                pilaSintactica.pop();
                continue;
            }
            
            if (tope.equals("807")){
                if (arregloActual != null) {
                    // Si es la primera vez que entramos, inicializamos los valores
                    if (arregloActual.dimensionArr == null) {
                        arregloActual.dimensionArr = 0;
                        arregloActual.tArr = ""; // Empezamos con un texto vacío
                        arregloActual.clase = "ARR";
                    }
                    
                    // Sumamos 1 a la dimensión
                    arregloActual.dimensionArr++;
                    
                    String numeroLeido = String.valueOf(tokenActual[1]);
        
                    // Si el texto está vacío (es el primer número), solo lo asignamos
                    if (arregloActual.tArr.isEmpty()) {
                        arregloActual.tArr = numeroLeido;
                    } 
                    // Si ya tiene números, le agregamos una coma y el nuevo número
                    else {
                        arregloActual.tArr = arregloActual.tArr + "," + numeroLeido;
                    }
                }
                pilaSintactica.pop();
                continue;
            }

            if (tope.equals("811")){
                leyendoOperacion = true;
                expresionActual.clear();
                lineaOperacionActual = (int) tokenActual[2]; // Asumiendo que [2] es la línea
                pilaSintactica.pop();
                continue;
            }
            if (tope.equals("812")) {
                leyendoOperacion = false;
                
                // Convertimos a prefijo el lado derecho
                List<Object[]> prefijaDer = convertirAPrefijo(expresionActual);
                
                // Creamos las listas completas (Izquierda + Derecha)
                List<Object[]> infijaCompleta = new ArrayList<>();
                List<Object[]> prefijaCompleta = new ArrayList<>();
                String idIzquierdoStr = (tokenIzquierdo != null) ? (String) tokenIzquierdo[1] : "";

                // Si la operación provino de una asignación (ej. x = 5), agregamos el lado izquierdo
                if (tokenIzquierdo != null && tokenAsignacion != null) {
                    // Armar Infija completa: [id] [=] [a] [+] [b]
                    infijaCompleta.add(tokenIzquierdo);
                    infijaCompleta.add(tokenAsignacion);
                    infijaCompleta.addAll(expresionActual);
                    
                    // Armar Prefija completa: [=] [id] [+] [a] [b]
                    prefijaCompleta.add(tokenAsignacion);
                    prefijaCompleta.add(tokenIzquierdo);
                    prefijaCompleta.addAll(prefijaDer);
                    
                    // Limpiamos temporales
                    tokenIzquierdo = null;
                    tokenAsignacion = null;
                } else {
                    // Era una condicional de un IF o WHILE (No hay lado izquierdo)
                    infijaCompleta.addAll(expresionActual);
                    prefijaCompleta.addAll(prefijaDer);
                }

                operacionesPrefijasTokens.add(prefijaCompleta);

                indiceGlobalPrefijo = 0; // <-- CRÍTICO: Reiniciar el contador

                int indexInicioCuadruplos = listaCuadruplos.size(); // Contamos cuántos hay antes
                // 1. Guardamos el estado exacto de los contadores ANTES de la línea
                int[] contadoresAntes = contadoresTemporales.clone();
                generarCuadruplosRecursivo(prefijaCompleta);

                // INICIO DE CAPTURA PARA EXCEL
                // ========================================================
                
                String ultimoTemporal = "";
                // Obtenemos directamente el último cuádruplo generado para sacar su temporal
                if (listaCuadruplos.size() > indexInicioCuadruplos) {
                    String[] ultimoCuad = listaCuadruplos.get(listaCuadruplos.size() - 1);
                    if (ultimoCuad.length > 3 && ultimoCuad[3] != null && ultimoCuad[3].startsWith("T")) {
                        ultimoTemporal = ultimoCuad[3];
                    }
                }

                String asignacionFinal = "-";
                if (!idIzquierdoStr.isEmpty() && !ultimoTemporal.isEmpty()) {
                    asignacionFinal = idIzquierdoStr + " = " + ultimoTemporal;
                } else if (!idIzquierdoStr.isEmpty()) {
                    asignacionFinal = idIzquierdoStr + " = (Valor directo)";
                } else if (!ultimoTemporal.isEmpty()) {
                    asignacionFinal = "Eval -> " + ultimoTemporal;
                }

                // 3. Calculamos la diferencia: cuántos se hicieron SOLAMENTE en esta línea
                int[] contadoresSoloEstaLinea = new int[9];
                for (int j = 0; j < 9; j++) {
                    contadoresSoloEstaLinea[j] = contadoresTemporales[j] - contadoresAntes[j];
                }

                // Guardamos: [Linea, ArregloContadores, AsignacionFinal]
                listaResumenOperaciones.add(new Object[]{lineaOperacionActual, contadoresSoloEstaLinea, asignacionFinal});
                // ========================================================
                // FIN DE CAPTURA PARA EXCEL
                // ========================================================



                StringBuilder textoInfijo = new StringBuilder();
                for (Object[] token : infijaCompleta) {
                    textoInfijo.append(token[1]).append(" "); // token[1] es el lexema ("x", "+", etc.)
                }

                // 2. Extraer los lexemas de la lista prefija
                StringBuilder textoPrefijo = new StringBuilder();
                for (Object[] token : prefijaCompleta) {
                    textoPrefijo.append(token[1]).append(" ");
                }

                // 3. Ahora sí, lo guardas o lo imprimes de forma legible
                String resultadoParaConsola = "linea: " + lineaOperacionActual + 
                                            " | prefijo : " + textoPrefijo.toString();

                registroOperaciones.add(resultadoParaConsola);
                //System.out.println(resultadoParaConsola);
                pilaSintactica.pop();
                continue;
            }

            if (tope.equals(terminalActual)){

                if (leyendoOperacion) {
                    expresionActual.add(tokenActual);
                } else {
                    if (terminalActual.equals("id")) {
                        tokenIzquierdo = tokenActual;
                    } else if (terminalActual.equals("=") || terminalActual.equals("+=") || 
                            terminalActual.equals("-=") || terminalActual.equals("*=") || terminalActual.equals("/=")) {
                        tokenAsignacion = tokenActual;
                    }
                }

                if (terminalActual.equals("id")){
                    String lexemaActual = (String) tokenActual[1];
                    int ambitoNum = pilaContadorAmbito.peek();

                    if (esTipoRegistro) {
                        tipoPersonalizado = lexemaActual; // Guardamos "MiRegistro"
                        esTipoRegistro = false;           // Apagamos la bandera
                    }
                    
                    else if (!claseActual.isEmpty()){
                        Map<String, Identificador> ambitoLocal = pilaIdentificadores.peek();
                        
                        if (ambitoLocal.containsKey(lexemaActual)){
                            String descripcion = "El identificador '" + lexemaActual + "' ya ha sido declarado en el ámbito " + ambitoNum; 
                            Object[] datosError = {542, descripcion, tokenActual[1], "Ambito", tokenActual[2],ambitoNum};
                            listaErroresSemantico.add(datosError);
                            
                        }
                        else{
                            String tipo = tipoPersonalizado.isEmpty() ? getTipo(tokenActual) : tipoPersonalizado;
                            Identificador nuevoId = new Identificador(lexemaActual, tipo, claseActual, ambitoNum);

                            if (claseActual.equals("FUNC")|| (claseActual.equals("REG") && esDefinicionRegistro)){
                                funcionActual = nuevoId;
                                nuevoId.numeroPar = 0;

                                nuevoId.tamañoPar = String.valueOf(numAmbito);
                            }
                            if (claseActual.equals("PAR")){
                                if (funcionActual != null){
                                    funcionActual.numeroPar++;
                                    nuevoId.numeroPar = funcionActual.numeroPar;
                                    nuevoId.tamañoPar = funcionActual.id;
                                }
                            }
                            if (claseActual.equals("VAR") || (claseActual.equals("REG") && !esDefinicionRegistro)){
                                arregloActual = nuevoId;
                                System.out.println(lexemaActual);
                                
                            }
                            ambitoLocal.put(lexemaActual, nuevoId);
                            registroIdentificadores.add(nuevoId);
                            //System.out.println(nuevoId);
                        }
                    }
                    else{
                        boolean existe = false;
                        for (Map<String, Identificador> ambito : pilaIdentificadores) {
                            if (ambito.containsKey(lexemaActual)) {
                                existe = true;
                                break;
                            }
                        }
                        if (!existe) {
                            String descripcion = "Error Semántico: '" + lexemaActual + "' no ha sido declarada. " + ambitoNum;
                            Object[] datosError = {543, descripcion, tokenActual[1], "Ambito", tokenActual[2],ambitoNum};
                            listaErroresSemantico.add(datosError);
                        }
                    }
                }

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
        if (!pilaContadorAmbito.isEmpty()){
            pilaContadorAmbito.pop();
        }
        if (pilaSintactica.isEmpty()) {
            System.out.println("¡Análisis sintáctico exitoso!");
        } else {
            System.out.println("El análisis terminó con errores (Pila no vacía).");
        }
        //CompilacionCuadruplos();
        actualizarListaErrores();
        //imprimirEstadisticasNoTerminales();
        // for (Object[] elemento : ListaCambiosAmbito) {
        //     System.out.println("linea: " + elemento[0] + " ambito: " + elemento[1] + " ocurrio = " + elemento[2]);
        // }
        // for (Identificador id : registroIdentificadores) {
        //     System.out.println(id);
        // }   
     }

        private int obtenerPrecedencia(String op) {
        switch (op) {
            case "||": return 1;
            case "&&": return 2;
            case "==": case "!=": return 3;
            case "<": case "<=": case ">": case ">=": return 4;
            case "+": case "-": return 5;
            case "*": case "/": case "%": return 6;
            case "^": return 7;
        }
        return -1;
    }
    private boolean esOperadorBinario(String op) {
        return op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%") || op.equals("^") ||
            op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=") || op.equals("==") || op.equals("!=") ||
            op.equals("&&") || op.equals("||") || op.equals("&") || op.equals("|");
    }

    private boolean esAsignacion(String op) {
        return op.equals("=") || op.equals("+=") || op.equals("-=") || op.equals("*=") || op.equals("/=");
    }

    public List<Object[]> convertirAPrefijo(List<Object[]> infija) {
        List<Object[]> invertida = new ArrayList<>();
        
        // 1. Invertir la infija y voltear los paréntesis lógicamente
        for (int i = infija.size() - 1; i >= 0; i--) {
            Object[] tokenOriginal = infija.get(i);
            String lexema = (String) tokenOriginal[1];
            
            if (lexema.equals("(")) {
                Object[] tokenMod = tokenOriginal.clone(); // Clonamos para no dañar el original
                tokenMod[1] = ")";
                invertida.add(tokenMod);
            } else if (lexema.equals(")")) {
                Object[] tokenMod = tokenOriginal.clone();
                tokenMod[1] = "(";
                invertida.add(tokenMod);
            } else {
                invertida.add(tokenOriginal);
            }
        }

        Stack<Object[]> pilaOperadores = new Stack<>();
        List<Object[]> prefijaInvertida = new ArrayList<>();

        // 2. Procesar con la Pila
        for (Object[] token : invertida) {
            String lexema = (String) token[1];

            if (lexema.equals("(")) {
                pilaOperadores.push(token);
            } else if (lexema.equals(")")) {
                while (!pilaOperadores.isEmpty() && !((String) pilaOperadores.peek()[1]).equals("(")) {
                    prefijaInvertida.add(pilaOperadores.pop());
                }
                if (!pilaOperadores.isEmpty()) pilaOperadores.pop(); // Sacar '('
            } else if (obtenerPrecedencia(lexema) != -1) {
                // Es operador
                while (!pilaOperadores.isEmpty() && 
                    obtenerPrecedencia((String) pilaOperadores.peek()[1]) > obtenerPrecedencia(lexema)) {
                    prefijaInvertida.add(pilaOperadores.pop());
                }
                pilaOperadores.push(token);
            } else {
                // Es operando (id, const)
                prefijaInvertida.add(token);
            }
        }

        while (!pilaOperadores.isEmpty()) {
            prefijaInvertida.add(pilaOperadores.pop());
        }

        // 3. Invertir el resultado final
        Collections.reverse(prefijaInvertida);
        return prefijaInvertida;
    }
    public void ejecutarCompilacionCuadruplos() {
        // 1. Limpiamos cuádruplos anteriores
        listaCuadruplos.clear();
        
        // 2. Reiniciamos los contadores de temporales por tipo
        for (int i = 0; i < contadoresTemporales.length; i++) {
            contadoresTemporales[i] = 0;
        }
        
        // 3. Recorremos cada línea de código y generamos los cuádruplos en memoria
        for (List<Object[]> operacion : operacionesPrefijasTokens) {
            indiceGlobalPrefijo = 0; 
            generarCuadruplosRecursivo(operacion); // Ya no se pasa 'out'
        }
        
        // ¡Listo! En este punto, 'listaCuadruplos' ya tiene todos los cuádruplos 
        // ordenados y listos para usarse en tu JTable, consola o siguiente fase.
        System.out.println("Cuádruplos generados en memoria exitosamente. Total: " + listaCuadruplos.size());
    }

    private String[] generarCuadruplosRecursivo(List<Object[]> prefijo) {
        if (indiceGlobalPrefijo >= prefijo.size()) {
            return new String[] {"", String.valueOf(VAR)};
        }
        Object[] tokenActual = prefijo.get(indiceGlobalPrefijo);
        String lexemaActual = (String) tokenActual[1];
        indiceGlobalPrefijo++;

        // CASO 1: Operador Binario
        if (esOperadorBinario(lexemaActual)) {
            String[] nodoIzq = generarCuadruplosRecursivo(prefijo);
            String[] nodoDer = generarCuadruplosRecursivo(prefijo);

            String val1 = nodoIzq[0];
            int tipo1 = Integer.parseInt(nodoIzq[1]);
            
            String val2 = nodoDer[0];
            int tipo2 = Integer.parseInt(nodoDer[1]);

            int tipoResultado = evaluarOperacion(tipo1, tipo2, lexemaActual);

            // ==========================================
            // ¡REGLA APLICADA! Incompatibilidad -> Variant
            // ==========================================
            if (tipoResultado == E) {
                //TODO ERROR SEMANTICO "ERROR '5+8' NO COMPATIBLE"
                String descripcion = "Incompatibilidad de tipos: no se puede aplicar '" + lexemaActual + 
                                "' entre " + NOMBRES[tipo1] + " y " + NOMBRES[tipo2];
                Object[] datosError = {544, descripcion, val1 + " " + lexemaActual +" "+ val2, "Semantico", tokenActual[2]};
                listaErroresSemantico1.add(datosError);
                
                // Guardas el error en tu lista global para imprimirlo en tu tabla/Excel
                // Object[] error = { ... }; 
                // listaErroresSemantico.add(error);
                
                //System.out.println("Error Semántico: " + descripcion);
                
                // Forzamos el tipo a Variant (8) para recuperar el sistema
                tipoResultado = VAR; 
            }

            // Se genera el temporal correcto (Si hubo error, generará un "TVX")
            String prefijoTemp = PREFIJOS_TEMP[tipoResultado];
            contadoresTemporales[tipoResultado]++;
            int numeroActual = contadoresTemporales[tipoResultado]; 

            String temporalAsignado = prefijoTemp + numeroActual;
            
            System.out.println(lexemaActual + "," + val1 + "," + val2 + "," + temporalAsignado);
            listaCuadruplos.add(new String[]{lexemaActual, nodoIzq[0], nodoDer[0], temporalAsignado});
            return new String[] {temporalAsignado, String.valueOf(tipoResultado)};
        } 
        // CASO 2: Asignación
        else if (esAsignacion(lexemaActual)) {
            String[] nodoIzq = generarCuadruplosRecursivo(prefijo);
            String[] nodoDer = generarCuadruplosRecursivo(prefijo);
            
            int tipoDestino = Integer.parseInt(nodoIzq[1]);
            int tipoValor = Integer.parseInt(nodoDer[1]);
            String val1 = nodoIzq[0];
            String val2 = nodoDer[0];
            
            // Validación opcional de asignación incompatible
            if (tipoDestino != tipoValor && tipoDestino != VAR && tipoValor != VAR) {
                // Aquí también podrías registrar error si intentan guardar un CAD en un DEC
                String descripcion = "Incompatibilidad de tipos: no se puede asignar '" + lexemaActual + 
                                "' entre " + NOMBRES[tipoDestino] + " y " + NOMBRES[tipoValor];
                Object[] datosError = {544, descripcion, val1 + " " + lexemaActual +" "+ val2, "Semantico", tokenActual[2]};
                listaErroresSemantico1.add(datosError);
            }

            System.out.println(lexemaActual + "," + nodoIzq[0] + "," + nodoDer[0]);
            return nodoIzq;
        } 
        // CASO 3: Operando (Variable o Número)
        else {
            int tipoLexema = obtenerTipoToken(tokenActual);
            if (tipoLexema == VAR){
                contadoresTemporales[VAR]++;
                int numeroActual = contadoresTemporales[VAR]; 
                
                // 2. Generamos la cadena del temporal (ej. "TV" + 1 -> "TV1")
                String temporalVariant = PREFIJOS_TEMP[VAR] + numeroActual;
                
                // 3. Retornamos el temporal EN LUGAR del lexema original
                return new String[] {temporalVariant, String.valueOf(tipoLexema)};
            }
            return new String[] {lexemaActual, String.valueOf(tipoLexema)};
        }
    }

    private int obtenerTipoToken(Object[] token) {
        int numToken = Integer.parseInt(token[0].toString());
        if (esUnIdentificador(numToken)){
            String lexema = (String) token[1];
            Identificador id = buscarEnTabla(lexema); // Lo buscamos en los ámbitos
            
            if (id != null) {
                // SÍ ESTÁ DECLARADA: Usamos el tipo que tiene registrado
                return convertirStringAInt(id.tipo);
            } else {
                // NO ESTÁ DECLARADA: ¡Se activa la trampa del Variant!
                return VAR; 
            }
        }
        else {
            // 2. Si NO es un ID, entonces obligatoriamente es una CONSTANTE (ej. 35, "Hola")
            // Como es constante, no necesita estar en la tabla, confiamos en getTipo.
            String tipoConstante = getTipo(token);
            if (!tipoConstante.isEmpty()) {
                // Es una constante (ej. token -62). Lo convertimos al índice de la matriz.
                return convertirStringAInt(tipoConstante);
            }
            return convertirStringAInt(tipoConstante);
        }
    }
    private boolean esUnIdentificador(int numToken){
        return (numToken <= -60 && numToken >= -67);
    }
    private int convertirStringAInt(String tipoStr) {
        if (tipoStr == null) return VAR;
        
        switch (tipoStr.toLowerCase()) {
            case "bin": return BIN; // 0
            case "dec": return DEC; // 1
            case "oct": return OCT; // 2
            case "hex": return HEX; // 3
            case "real": return REAL; // 4
            case "exp": return EXP; // 5
            case "cadena": return CAD; // 6
            case "boolean": return BOOL; // 7
            default: return VAR; // 8
        }
    }

    private int evaluarOperacion(int tipo1, int tipo2, String operador) {
       // La lógica del tipo VAR (índice 8) ya está mapeada automáticamente 
        // dentro de los arreglos bidimensionales, no se requieren 'if' extra.

        if (operador.equals("+") || operador.equals("+=")) return SUMA[tipo1][tipo2];
        if (operador.equals("-") || operador.equals("-=")) return RESTA[tipo1][tipo2];
        if (operador.equals("*") || operador.equals("*=")) return MULT[tipo1][tipo2];
        if (operador.equals("/") || operador.equals("/=")) return DIV[tipo1][tipo2];
        //POT
        if (operador.equals("%") || operador.equals(">>>") || operador.equals(">>") || operador.equals("<<") || operador.equals("^")) return POT[tipo1][tipo2];
        // Operadores Relacionales de Magnitud
        if (operador.equals("<") || operador.equals("<=") || operador.equals(">") || operador.equals(">=")) {
            return REL[tipo1][tipo2];
        }
        // Operadores de Igualdad y Desigualdad
        if (operador.equals("==") || operador.equals("!=")) {
            // Excepciones donde '==' y '!=' SÍ son válidos (según la segunda hoja de Excel):
            if (tipo1 == CAD && tipo2 == CAD) return BOOL;   // Cadena == Cadena
            if (tipo1 == BOOL && tipo2 == BOOL) return BOOL; // Boolean == Boolean
            if (tipo1 == VAR && tipo2 == BOOL) return BOOL;  // Variant == Boolean
            if (tipo1 == BOOL && tipo2 == VAR) return BOOL;  // Boolean == Variant
            
            // Para cualquier otro caso, se comportan igual que la matriz REL base
            return REL[tipo1][tipo2];
        }
        // Operadores Lógicos
        if (operador.equals("&&") || operador.equals("||") || operador.equals("&") || operador.equals("|") || operador.equals("#")) {
            return LOG[tipo1][tipo2];
        }

        return E; // Retorna Error si el operador no existe o hay un fallo inesperado
    }

    private Identificador buscarEnTabla(String lexemaActual) {
        for (Map<String, Identificador> ambito : pilaIdentificadores) {
            if (ambito.containsKey(lexemaActual)) {
                return ambito.get(lexemaActual); // Found it! Return the Identifier object.
            }
        }
        return null;
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
        fc.setSelectedFile(new File("Semantica1-BrandonFornesRubio.xlsx"));
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
            if (listaErroresSemantico != null) {
                erroresUnificados.addAll(listaErroresSemantico);
            }
            if (listaErroresSemantico1 != null) {
                erroresUnificados.addAll(listaErroresSemantico1);
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
            // =========================================================================
            // 5. NUEVA HOJA: ÁMBITO (Contador por cada tipo de ID y Totales)
            // =========================================================================
            Sheet hojaAmbito = workbook.createSheet("Ámbito");
            // Se eliminó "Char", por lo que "Errores" es índice 9 y "total" es 10
            String[] cabeceraAmbito = {"Ambito", "Bin", "Dec", "Oct", "Hex", "Real", "exp", "Cadena", "Boolean", "Errores", "total"};
            crearFilaCabecera(hojaAmbito, cabeceraAmbito, estiloCabecera);

            // a) Determinar el ámbito máximo (para saber cuántas filas crear)
            int maxAmbito = 0;
            for (Identificador id : registroIdentificadores) {
                if (id.ambito > maxAmbito) maxAmbito = id.ambito;
            }
            if (listaErroresSemantico != null) {
                for(Object[] err : listaErroresSemantico) {
                    // Validamos si guardaste el ámbito en la posición 5
                    if (err.length > 5 && err[5] instanceof Integer) {
                        int ambError = (Integer) err[5];
                        if (ambError > maxAmbito) maxAmbito = ambError;
                    }
                }
            }

            // b) Matrices para contar (Filas = Ambitos, Columnas = Tipos)
            // Se eliminó "Char" del arreglo de validación
            String[] tiposColumna = {"Bin", "Dec", "Oct", "Hex", "Real", "exp", "Cadena", "Boolean"};
            int[][] matrizTipos = new int[maxAmbito + 1][tiposColumna.length];
            int[] matrizErrores = new int[maxAmbito + 1];

            // Rellenar matrices
            for (Identificador id : registroIdentificadores) {
                for (int j = 0; j < tiposColumna.length; j++) {
                    if (tiposColumna[j].equalsIgnoreCase(id.tipo)) {
                        matrizTipos[id.ambito][j]++;
                        break;
                    }
                }
            }
            if (listaErroresSemantico != null) {
                for(Object[] err : listaErroresSemantico) {
                    if (err.length > 5 && err[5] instanceof Integer) {
                        matrizErrores[(Integer) err[5]]++;
                    }
                }
            }

            // c) Imprimir datos en la hoja y calcular totales
            int[] totalesTipos = new int[tiposColumna.length];
            int totalErrores = 0;
            int granTotal = 0;

            for (int i = 0; i <= maxAmbito; i++) {
                Row fila = hojaAmbito.createRow(i + 1);
                fila.createCell(0).setCellValue(i); // Nivel de Ámbito
                
                int totalFila = 0;
                // Escribir los tipos (ahora son 8) y sumarlos a los totales de columna
                for (int j = 0; j < tiposColumna.length; j++) {
                    fila.createCell(j + 1).setCellValue(matrizTipos[i][j]);
                    
                    totalFila += matrizTipos[i][j];
                    totalesTipos[j] += matrizTipos[i][j]; // Suma acumulada de esta columna
                }
                
                // Escribir Errores (Índice 9) y sumarlos
                fila.createCell(9).setCellValue(matrizErrores[i]);
                //totalFila += matrizErrores[i];
                totalErrores += matrizErrores[i];
                
                // Escribir Total por fila (Índice 10)
                fila.createCell(10).setCellValue(totalFila);
                granTotal += totalFila;
            }

            // d) Fila extra para los TOTALES FINALES
            Row filaTotal = hojaAmbito.createRow(maxAmbito + 2);
            filaTotal.createCell(0).setCellValue("total"); // Etiqueta
            
            // Imprimir los totales de cada columna de tipo
            for (int j = 0; j < tiposColumna.length; j++) {
                filaTotal.createCell(j + 1).setCellValue(totalesTipos[j]);
            }
            
            // Imprimir total general de errores (Índice 9) y el gran total final (Índice 10)
            filaTotal.createCell(9).setCellValue(totalErrores);
            filaTotal.createCell(10).setCellValue(granTotal);
            
            // Ponerle negritas a la fila de totales para que resalte
            for (int k = 0; k <= 10; k++) {
                Cell celda = filaTotal.getCell(k);
                if (celda != null) {
                    celda.setCellStyle(estiloCabecera);
                }
            }
            
            // Auto-ajustar columnas
            for (int i = 0; i < cabeceraAmbito.length; i++) {
                hojaAmbito.autoSizeColumn(i);
            }
            // =========================================================================


            // =========================================================================
            // 6. NUEVA HOJA: TABLA DE SÍMBOLOS
            // =========================================================================
            Sheet hojaSimbolos = workbook.createSheet("Tabla de Simbolos");
            String[] cabeceraSimbolos = {"id", "tipo", "Clase", "amb", "Tarr", "DimArr", "NoPar", "TParr"};
            crearFilaCabecera(hojaSimbolos, cabeceraSimbolos, estiloCabecera);

            int filaIndex = 1;
            for (Identificador id : registroIdentificadores) {
                Row fila = hojaSimbolos.createRow(filaIndex++);
                
                // Aplicamos la regla: Si es null, imprimimos "-", si no, imprimimos su valor
                fila.createCell(0).setCellValue(id.id != null ? id.id : "-");
                fila.createCell(1).setCellValue(id.tipo != null ? id.tipo : "-");
                fila.createCell(2).setCellValue(id.clase != null ? id.clase : "-");
                fila.createCell(3).setCellValue(id.ambito); // Es primitivo (int), nunca es null

                fila.createCell(4).setCellValue(id.tArr != null ? id.tArr.toString() : "-");
                fila.createCell(5).setCellValue(id.dimensionArr != null ? id.dimensionArr.toString() : "-");
                fila.createCell(6).setCellValue(id.numeroPar != null ? id.numeroPar.toString() : "-");
                
                // Usamos toString() para el tamañoPar por si está guardando Integers o Strings (nombres de función)
                fila.createCell(7).setCellValue(id.tamañoPar != null ? id.tamañoPar.toString() : "-");
            }

            for (int i = 0; i < cabeceraSimbolos.length; i++) hojaSimbolos.autoSizeColumn(i);
            // =========================================================================
            // =========================================================================
            // 7. NUEVA HOJA: RESUMEN DE LÍNEAS (Temporales, Asignación y Errores)
            // =========================================================================
            Sheet hojaResumen = workbook.createSheet("Semantica 1");
            // Cabecera mapeada a tus constantes: BIN(0), DEC(1), OCT(2), HEX(3), REAL(4), EXP(5), CAD(6), BOOL(7), VAR(8)
            String[] cabeceraResumen = {"Línea", "TBin", "TDec", "TOct", "THex", "TReal", "TExp", "TCad", "TBool", "TVar", "Asignación Final", "Errores Semánticos"};
            crearFilaCabecera(hojaResumen, cabeceraResumen, estiloCabecera);

            // a) Agrupar errores semánticos por número de línea
            Map<Integer, Integer> erroresPorLinea = new HashMap<>();
            if (listaErroresSemantico1 != null) {
                for (Object[] error : listaErroresSemantico1) {
                    if (error.length > 4 && error[4] instanceof Integer) {
                        int linea = (Integer) error[4];
                        erroresPorLinea.put(linea, erroresPorLinea.getOrDefault(linea, 0) + 1);
                    }
                }
            }

            int filaResumenIndex = 1;
            // --- VARIABLES PARA LA SUMATORIA FINAL ---
            int[] totalesTemporales = new int[9];
            int totalAsignaciones = 0;
            int granTotalErrores = 0;

            // b) Escribir las filas basadas en las operaciones procesadas
            for (Object[] resumen : listaResumenOperaciones) {
                Row fila = hojaResumen.createRow(filaResumenIndex++);
                
                int linea = (Integer) resumen[0];
                int[] contadores = (int[]) resumen[1];
                String asignacion = (String) resumen[2];
                int numErrores = erroresPorLinea.getOrDefault(linea, 0);

                fila.createCell(0).setCellValue(linea);
                
                // Vaciamos los 9 contadores en sus respectivas celdas (Columnas 1 a la 9)
                for (int j = 0; j < 9; j++) {
                    fila.createCell(j + 1).setCellValue(contadores[j]);
                    totalesTemporales[j] += contadores[j];
                }
                
                fila.createCell(10).setCellValue(asignacion != null ? asignacion : "-");
                // Si hubo una asignación real, sumamos 1 al contador de asignaciones
                if (asignacion != null && !asignacion.equals("-")) {
                    totalAsignaciones++;
                }
                fila.createCell(11).setCellValue(numErrores);
                granTotalErrores += numErrores; // Sumamos errores

                erroresPorLinea.remove(linea); // Marcamos como procesada
                
            }
            // d) --- CREAR LA FILA DE TOTALES ---
            Row filaTotales = hojaResumen.createRow(filaResumenIndex++);
            filaTotales.createCell(0).setCellValue("Totales");
            
            // Imprimir la sumatoria de cada tipo de temporal
            for (int j = 0; j < 9; j++) {
                filaTotales.createCell(j + 1).setCellValue(totalesTemporales[j]);
            }
            
            // Imprimir el total de asignaciones y el total general de errores
            filaTotales.createCell(10).setCellValue(totalAsignaciones);
            filaTotales.createCell(11).setCellValue(granTotalErrores);

            // Aplicarle negritas (estiloCabecera) a toda la fila para que resalte
            for (int k = 0; k <= 11; k++) {
                Cell celda = filaTotales.getCell(k);
                if (celda != null) {
                    celda.setCellStyle(estiloCabecera);
                }
            }

            // Auto-ajustar columnas
            for (int i = 0; i < cabeceraResumen.length; i++) {
                hojaResumen.autoSizeColumn(i);
            }
            // =========================================================================

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
    
    // private void ExportarCuadruplosTXT() {
    //     JFileChooser fc = new JFileChooser();
    //     fc.setDialogTitle("Exportar Cuádruplos a TXT");
    //     fc.setFileFilter(new FileNameExtensionFilter("Archivo de Texto (*.txt)", "txt"));
    //     fc.setSelectedFile(new File("Semantica-Avance 2-BrandonFornes")); 

    //     if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
    //         File archivo = fc.getSelectedFile();
    //         if (!archivo.getName().toLowerCase().endsWith(".txt")) {
    //             archivo = new File(archivo.getParentFile(), archivo.getName() + ".txt");
    //         }

    //         try (FileWriter fw = new FileWriter(archivo);
    //             BufferedWriter bw = new BufferedWriter(fw);
    //             PrintWriter out = new PrintWriter(bw)) {
                
    //             int lineaAnterior = -1;

    //             for (int i = 0; i < contadoresTemporales.length; i++) {
    //                 contadoresTemporales[i] = 0;
    //             }
    //             for (List<Object[]> operacion : operacionesPrefijasTokens) {
                    
    //                 int lineaActual = (int) operacion.get(0)[2];
                    
    //                 // Imprimir el encabezado solo una vez por línea
    //                 if (lineaActual != lineaAnterior) {
    //                     StringBuilder prefijoStr = new StringBuilder();
    //                     for (Object[] token : operacion) {
    //                         prefijoStr.append(token[1]).append(" ");
    //                     }
    //                     out.println("Línea: " + lineaActual + " | " + prefijoStr.toString().trim());
    //                     lineaAnterior = lineaActual; 
    //                 }

    //                 // Reiniciamos el índice apuntador para empezar a leer esta línea desde el 0
    //                 indiceGlobalPrefijo = 0; 
                    
    //                 // ==========================================
    //                 // LÓGICA RECURSIVA PURA
    //                 // ==========================================

    //                 //generarCuadruplosRecursivo(operacion, out);
                    
    //                 out.println(); // Salto de línea para separar la siguiente ecuación
    //             }

    //             JOptionPane.showMessageDialog(this, "Exportado correctamente.", 
    //                 "Exportación Exitosa", JOptionPane.INFORMATION_MESSAGE);

    //         } catch (IOException e) {
    //             JOptionPane.showMessageDialog(this, "Error al guardar el archivo:\n" + e.getMessage(), 
    //                 "Error", JOptionPane.ERROR_MESSAGE);
    //         }
    //     }
    // }

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
        SwingUtilities.invokeLater(AnalizadorPrincipal::new);
    }
}
