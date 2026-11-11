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
    private ArrayList<Object[]> listaErroresSemantico2 = new ArrayList<>();
    // Variables ambito
    private ArrayList<Object[]> ListaCambiosDeclaracion = new ArrayList<>();
    private ArrayList<Object[]> ListaCambiosAmbito = new ArrayList<>();
    private Stack<Integer> pilaContadorAmbito = new Stack<>();
    private Deque<Map<String, Identificador>> pilaIdentificadores = new ArrayDeque<>();;
    private List<Identificador> registroIdentificadores = new ArrayList<>();
    public boolean EsDeclaracion = true;

    private int indiceGlobalPrefijo = 0;
    
    //Variables SEMANTICA
    boolean leyendoOperacion = false;
    boolean ignorarParaExcel = false;
    List<Object[]> expresionActual = new ArrayList<>();
    int lineaOperacionActual = 0;
    public List<String> registroOperaciones = new ArrayList<>();
    public List<List<Object[]>> operacionesPrefijasTokens = new ArrayList<>();
    public List<Object[]> listaResumenOperaciones = new ArrayList<>();
    // Lista global para guardar los registros de las reglas
    public List<Object[]> listaRegistroReglas = new ArrayList<>();  

    Object[] tokenIzquierdo = null;
    Object[] tokenAsignacion = null;

    //VARIABLES SEMANTICA 2
    boolean enFuncion = false;
    boolean enProcedimiento = false;
    String idMetodoActual = "";
    String tipoMetodoActual = "";
    boolean encontroReturn = false;
    String lineaDelReturn = ""; // <-- NUEVA VARIABLE
    Object lineaMetodoActual = ""; // Linea del "def" (para la regla 1140)

    // Pila de defs abiertas: una def anidada ya no apaga las banderas de la def que la contiene
    static class CtxMetodo {
        String id = "";
        String tipo = "";
        boolean esFuncion;
        Object linea = "";
        boolean encontroReturn;
        int profundidad;          // tamaño de pilaContadorAmbito dentro de la def (su propio ambito)
    }
    Deque<CtxMetodo> pilaMetodos = new ArrayDeque<>();
    boolean ultimoIdDuplicado = false;   // el ultimo id declarado ya existia en su ambito
    boolean ladoDerechoVoid = false;
    boolean ladoIzquierdoArreglo = false; // la sentencia empieza con un arreglo (ej. #DL[1] = ...)// la asignacion en curso recibe un valor void (CLEAR, procedimiento...)


    int nivelDeCorchetes = 0;

    //VARIABLES SEMANTICA 3
    //TODO CLEAR Y TODO
    List<Object[]> listaErroresSemantico3 = new ArrayList<>();


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
        {"def","805", "id","802","835","LISTA DE PARAMETROS", "PROGRAMA","803", ";","810", "A1"},
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
        {",","821", "OR","820","839", "B1"},// INDICE 30 
        {"Console.log", "(", "OR", ")"},
        {"823","if", "(","818", "OR","834", ")", "STATU", "B2"},
        {"823","elseif", "(","818", "OR","834", ")", "STATU", "B2"},
        {"else", "STATU"},
        {"OR"},
        {"{", "STATU", "A2", "}"},
        {"823","while", "(","818", "OR","834", ")", "STATU"},
        {"823","do", "STATU", "while", "(","818", "OR","834", ")"},
        {"return","836", "OR","837"},
        {"for","816", "(","802","801","818", "OR", "B3", ")","817", "STATU","803"},
        {",", "OR", "B3"},
        {":","800","829", "OR","830"},
        {";","831","800", "STATU",";","832", "OR","833", "B1"},
        {"switch", "(","818", "OR","824", ")", "{", "case","818", "OR","825", ":", "STATU", "B4", "}"},
        {";", "STATU", "B4"},
        {"break", "B5"},
        {"case","818", "OR","825", ":", "STATU", "B4"},
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
        {"(","838", "I4", ")","840"}, // Índice 80: ( I4 )
        {"ASIG","811", "OR","812", "I3"}, // Índice 81: ASIG OR I3
        {"?", "OR", ":", "OR"}, // Índice 82: ? OR : OR
        {"OR","839", "B1"}, // Índice 83: OR B1
        {"++","id","826", "I1"}, // Índice 84: ++ id I1
        {"--","id","826", "I1"}, // Índice 85: -- id I1
        {"(", "OR", ")"}, // Índice 86: ( OR )
        {"!", "(","818", "OR","827", ")"}, // Índice 87: ! ( OR )
        {"~", "(","818", "OR","828", ")"}, // Índice 88: ~ ( OR )
        {"FUNCION"}, // Índice 89: FUNCION
        {"[","819", "OR","820", "B1", "]","822"}, // Índice 90: [ OR B1 ]
        {"CLEAR", "850", "853"}, // Índice 91: CLEAR
        {"SQRT", "850", "(", "851", "OR", "852", ")", "853"}, // Índice 92: SQRT ( OR )
        {"POW", "850", "(", "851", "OR", "852", ",", "851", "OR", "852", ")", "853"}, // Índice 93: POW ( OR , OR )
        {"SQRTV", "850", "(", "851", "OR", "852", ",", "851", "OR", "852", ")", "853"}, // Índice 94: SQRTV ( OR , OR )
        {"STRLEN", "850", "(", "851", "OR", "852", ")", "853"}, // Índice 95: STRLEN ( OR )
        {"concat", "850", "(", "851", "OR", "852", ",", "851", "OR", "852", ")", "853"}, // Índice 96: concat ( OR , OR )
        {"copy", "850", "(", "851", "OR", "852", ",", "851", "OR", "852", ")", "853"}, // Índice 97: copy ( OR , OR )
        {"val", "850", "(", "851", "OR", "852", ",", "851", "OR", "852", ",", "851", "OR", "852", ")", "853"}, // Índice 98: val ( OR , OR , OR )
        {"str", "850", "(", "851", "OR", "852", ",", "851", "OR", "852", ")", "853"}, // Índice 99: str ( OR , OR )
        {"sin", "850", "(", "851", "OR", "852", ")", "853"}, // Índice 100: sin ( OR )
        {"cos", "850", "(", "851", "OR", "852", ")", "853"}, // Índice 101: cos ( OR )
        {"tan", "850", "(", "851", "OR", "852", ")", "853"}, // Índice 102: tan ( OR )
        {"chr", "850", "(", "851", "OR", "852", ")", "853"}, // Índice 103: chr ( OR )
        {"pred", "850", "(", "851", "OR", "852", ")", "853"}, // Índice 104: pred ( OR )
        {"succ", "850", "(", "851", "OR", "852", ")", "853"}, // Índice 105: succ ( OR )
        {"inc", "850", "(", "851", "OR", "852", ")", "853"}, // Índice 106: inc ( OR )
        {"dec", "850", "(", "851", "OR", "852", ")", "853"}, // Índice 107: dec ( OR )
        {"sqr", "850", "(", "851", "OR", "852", ")", "853"}, // Índice 108: sqr ( OR )
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

    public String getTipo(Object[] token) {
        if (token == null || token.length == 0 || token[0] == null) return "";
        
        try {
            int numToken = Integer.parseInt(token[0].toString());
            switch (numToken) {
                case -53: case -60: return "Cadena";
                case -54: case -61: return "Bin";
                case -57: case -62: return "Dec";
                case -55: case -63: return "Oct";
                case -56: case -64: return "Hex";
                case -58: case -65: return "Real";
                case -59: case -66: return "exp";
                case -67: case -69: case -70: return "Boolean";
                case 0: return "Variant";
                default: break;
            }
        } catch (NumberFormatException e) {
            // El token es un "id" o un operador (no un número). 
            // Se ignora el fallo para que la validación lo busque en la tabla de símbolos.
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
        
        // JButton btntxtAvance1Semantica2 = new JButton("Crear txt Avance 3");
        // btntxtAvance1Semantica2.addActionListener(e -> ExportarReglasTXT());
        
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
        // bar.add(btntxtAvance1Semantica2);
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
        for (Object[] tokenErrorSemantico2 : listaErroresSemantico2) {
            errorTableModel.addRow(tokenErrorSemantico2);
        }
        for (Object[] tokenErrorSemantico3 : listaErroresSemantico3) {
            errorTableModel.addRow(tokenErrorSemantico3);
        }

        // La celda ERRORES del conteo suma todas las categorias: lexicos, sintacticos, ambito
        // y semantica 1, 2 y 3. Solo se actualiza la tabla de la interfaz (el arreglo 'categorias',
        // que usa el Excel, no se modifica).
        int totalErrores = listaErrores.size() + listaErroresSintactico.size() + listaErroresSemantico.size()
                         + listaErroresSemantico1.size() + listaErroresSemantico2.size() + listaErroresSemantico3.size();
        counterTableModel.setValueAt(totalErrores, ERRORES, 1);
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
        listaErroresSemantico2.clear();        
        listaErroresSemantico3.clear();
        pilaCtx.clear();
        listaCuadruplos.clear();
        nivelDeCorchetes = 0;
        leyendoOperacion = false;
        expresionActual.clear();
        tokenIzquierdo = null;
        tokenAsignacion = null;
        enFuncion = false;
        enProcedimiento = false;
        encontroReturn = false;
        pilaMetodos.clear();
        ultimoIdDuplicado = false;
        idMetodoActual = "";
        tipoMetodoActual = "";
        ignorarParaExcel = false;
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
        String valorDerechoStr = "";
        //SEMANTICA 2
        List<Object[]> tokensDimensiones = new ArrayList<>();
        String ultimoIdVisto = "";
        String estructuraEnUso = "";
        String tipoSwitchActual = "";
        String lexemaSwitchActual = "";
        String idIteradorFor = "";
        boolean huboAsignacionFor = false;
        String lexemaAsignacionFor = "";
        int parteForClasico = 0;  // <--- AQUÍ ESTÁ
        String idIzquierdoStr = "";
        boolean vieneDeArreglo = false;

        pilaIdentificadores.clear();
        pilaIdentificadores.push(new HashMap<>());
        registroIdentificadores.clear();
        registroOperaciones.clear();
        operacionesPrefijasTokens.clear();
        listaResumenOperaciones.clear();
        listaRegistroReglas.clear();
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
                // Insertar esto DENTRO de tu Acción 803 actual, antes del pop()
                int ambitoActual = pilaContadorAmbito.isEmpty() ? 0 : pilaContadorAmbito.peek();
                // Se cierra una def solo si el ambito que se cierra es el de la def de arriba
                // (el 803 tambien cierra el ambito de un for o de un registro).
                boolean cierraDef = !pilaMetodos.isEmpty() && pilaMetodos.peek().profundidad == pilaContadorAmbito.size();
                if (cierraDef) {
                    CtxMetodo m = pilaMetodos.pop();
                    // Regla 1130: Toda función debe tener al menos un retorno (se marca en la linea del def)
                    if (m.esFuncion) {
                        // El 1130 "Acept" ya se registro en la accion 837, en el primer return
                        if (!m.encontroReturn) {
                            String errorMsg = "La función declarada debe contener por lo menos un retorno";
                            listaRegistroReglas.add(new Object[]{m.linea, "1130", "RETURN DE FUNCION", "Error", m.id, ambitoActual,"return"});
                            listaErroresSemantico2.add(new Object[]{561, errorMsg, m.id, "Semantico 2", m.linea});
                        }
                    }
                    // Regla 1140: Procedimiento no debe tener retorno (Verificación final de Acept)
                    else if (!m.encontroReturn) {
                        listaRegistroReglas.add(new Object[]{m.linea, "1140", "NO RETURN DE PROCEDIMIENTO", "Acept", m.id, ambitoActual, "TVoid"});
                    }
                    cargarMetodoActual();   // vuelve a la def que la contiene (o a main)
                }

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
            if (tope.equals("816")) {
                ignorarParaExcel = true; 
                huboAsignacionFor = false; // NUEVO
                lexemaAsignacionFor = "";  // NUEVO
                parteForClasico = 1;
                pilaSintactica.pop();
                continue;
            }
            if (tope.equals("817")) {
                ignorarParaExcel = false; 

                leyendoOperacion = false;      // Forzamos a salir del modo matemático
                expresionActual.clear();       // Vaciamos los tokens fantasma como "#Dk = #Dk + 1"
                tokenIzquierdo = null;
                tokenAsignacion = null;
                valorDerechoStr = ""; // <-- ¡Aquí también se reinicia!
                pilaSintactica.pop();
                continue;
            }
            if (tope.equals("818")){
                // 1. Encendemos la recolección de tokens matemáticos/lógicos
                leyendoOperacion = true;
                
                // 2. Limpiamos cualquier basura que haya quedado en memoria
                expresionActual.clear();
                tokenIzquierdo = null;
                tokenAsignacion = null;
                valorDerechoStr = "";
                pilaSintactica.pop();
                continue;
            }

            // =====================================================================
            // Acción 819: Inicia el Arreglo (Abre Corchete)
            // Se abre un contexto propio en pilaCtx, asi un arreglo dentro de otro
            // (o dentro de una funcion) no pisa el contexto de afuera.
            // =====================================================================
            if (tope.equals("819")){
                nivelDeCorchetes++;
                CtxArreglo c = new CtxArreglo();
                c.id = ultimoIdVisto;
                c.arr = buscarEnTabla(ultimoIdVisto);
                Object[] tokId = (i >= 2) ? listaTokens.get(i - 2) : null;   // i-1 = "[" , i-2 = id
                c.codigoId = (tokId != null && String.valueOf(tokId[1]).equals(c.id)) ? tokId[0] : -142;
                c.linea = tokenActual[2];
                c.leyendoPrev = leyendoOperacion;
                c.valorDerPrev = valorDerechoStr;
                c.exprPrevia = c.leyendoPrev ? sinApertura(expresionActual, c.id, "[") : new ArrayList<>();
                pilaCtx.push(c);

                idIzquierdoStr = ultimoIdVisto;
                vieneDeArreglo = true;

                expresionActual.clear();
                leyendoOperacion = true;
                valorDerechoStr = "";
                pilaSintactica.pop();
                continue;
            }

            // =====================================================================
            // Acción 820: Evaluar Dimensión Individual del Arreglo
            // Solo actua si lo mas interno abierto es un arreglo (en una llamada
            // de usuario la coma tambien dispara 820 y ahi no debe hacer nada).
            // =====================================================================
            if (tope.equals("820")) {
                if (!pilaCtx.isEmpty() && pilaCtx.peek() instanceof CtxArreglo) {
                    CtxArreglo c = (CtxArreglo) pilaCtx.peek();
                    Identificador arregloEnUso = c.arr;

                    if (arregloEnUso != null && !expresionActual.isEmpty()) {
                        c.dims++;
                        int contadorDimensionesUso = c.dims;
                        String valorFinalDimension = "";
                        Object[] tokenEvaluado = null;

                        // 1. Resolver el valor de la dimension
                        if (expresionActual.size() == 1) {
                            tokenEvaluado = expresionActual.get(0);
                            valorFinalDimension = String.valueOf(tokenEvaluado[1]);
                            if (etiqueta(tokenEvaluado).equals("VOID")) {
                                valorFinalDimension = "TVoid";
                            } else if (etiqueta(tokenEvaluado).isEmpty() && esIdentificadorLexema(valorFinalDimension) && buscarEnTabla(valorFinalDimension) == null) {
                                contadoresTemporales[VAR]++;
                                valorFinalDimension = PREFIJOS_TEMP[VAR] + contadoresTemporales[VAR];
                                tokenEvaluado = new Object[]{0, valorFinalDimension, tokenEvaluado[2]};
                            }
                        } else {
                            List<Object[]> prefijaDer = convertirAPrefijo(expresionActual);
                            indiceGlobalPrefijo = 0;
                            int indexInicioCuadruplos = listaCuadruplos.size();
                            generarCuadruplosRecursivo(prefijaDer);

                            if (listaCuadruplos.size() > indexInicioCuadruplos) {
                                String[] ultimoCuad = listaCuadruplos.get(listaCuadruplos.size() - 1);
                                if (ultimoCuad.length > 3 && ultimoCuad[3] != null && ultimoCuad[3].startsWith("T")) {
                                    valorFinalDimension = ultimoCuad[3];
                                }
                            }
                            if (valorFinalDimension.isEmpty() && valorDerechoStr != null && !valorDerechoStr.isEmpty()) {
                                valorFinalDimension = valorDerechoStr;
                            }
                            Object origenLinea = (tokenActual != null && tokenActual.length > 2) ? tokenActual[2] : "";
                            tokenEvaluado = new Object[]{"id", valorFinalDimension, origenLinea};
                        }

                        // 2. VALIDACIÓN (REGLAS 1030, 1040 y 1050)
                        // Un id que no es arreglo no se puede indexar: 1030 con "Dim 0" (una sola vez)
                        if (arregloEnUso.dimensionArr == null && contadorDimensionesUso == 1) {
                            int ambitoNoArr = pilaContadorAmbito.isEmpty() ? 0 : pilaContadorAmbito.peek();
                            listaRegistroReglas.add(new Object[]{tokenActual[2], "1030", "CANT. DIMENSIONES", "Error", arregloEnUso.id, ambitoNoArr, "Dim 0"});
                            listaErroresSemantico2.add(new Object[]{546, "'" + arregloEnUso.id + "' no es un arreglo", arregloEnUso.id, "Semantico 2", tokenActual[2]});
                        }
                        if (arregloEnUso.dimensionArr != null) {
                            String cveRegla3 = "1030";
                            String descRegla3 = "CANT. DIMENSIONES";
                            String cveRegla4 = "1040";
                            String descRegla4 = "EXP. ES ENTERA";
                            int ambitoActual = pilaContadorAmbito.isEmpty() ? 0 : pilaContadorAmbito.peek();
                            String topeDim = "Dim " + arregloEnUso.dimensionArr;

                            Object tokenLinea = (tokenEvaluado != null && tokenEvaluado.length > 2) ? tokenEvaluado[2] : "";

                            // Regla 1030
                            if (contadorDimensionesUso > arregloEnUso.dimensionArr) {
                                String descFallo = "Se excedió la cantidad de dimensiones declaradas (" + arregloEnUso.dimensionArr + ")";
                                listaRegistroReglas.add(new Object[]{tokenLinea, cveRegla3, descRegla3, "Error", arregloEnUso.id, ambitoActual, topeDim});
                                listaErroresSemantico2.add(new Object[]{546, descFallo, arregloEnUso.id, "Semantico 2", tokenLinea});
                            } else {
                                listaRegistroReglas.add(new Object[]{tokenLinea, cveRegla3, descRegla3, "Acept", arregloEnUso.id, ambitoActual, topeDim});

                                // Regla 1040
                                boolean esEntera = false;
                                if (esTemporalNombre(valorFinalDimension)) {
                                    String tipoTemp = getTipo(tokenEvaluado);
                                    // El indice debe ser Dec (Bin/Oct/Hex no se aceptan, igual que la prueba de referencia)
                                    if (tipoTemp.isEmpty()) {
                                        esEntera = valorFinalDimension.startsWith("TD") || valorFinalDimension.startsWith("TV");
                                    } else {
                                        esEntera = tipoTemp.equals("Dec") || tipoTemp.equals("Variant");
                                    }
                                } else {
                                    String tipo = getTipo(tokenEvaluado);
                                    if (tipo.equals("Dec") || (tipo.isEmpty() && valorFinalDimension.startsWith("#D"))) {
                                        esEntera = true;
                                    }
                                }

                                if (!esEntera) {
                                    listaRegistroReglas.add(new Object[]{tokenLinea, cveRegla4, descRegla4, "Error", valorFinalDimension, ambitoActual, "TDec"});
                                    listaErroresSemantico2.add(new Object[]{547, descRegla4, arregloEnUso.id, "Semantico 2", tokenLinea});
                                } else {
                                    listaRegistroReglas.add(new Object[]{tokenLinea, cveRegla4, descRegla4, "Acept", valorFinalDimension, ambitoActual, "TDec"});

                                    // Regla 1050
                                    String cveRegla5 = "1050";
                                    String descRegla5 = "INDICE EN RANGO";
                                    String[] limitesDeclarados = arregloEnUso.tArr.split(",");
                                    int indiceActual = contadorDimensionesUso - 1;

                                    if (indiceActual < limitesDeclarados.length) {
                                        int limiteMaximo = Integer.parseInt(limitesDeclarados[indiceActual].trim());
                                        String topeRango = "0.." + (limiteMaximo - 1);
                                        boolean enRango = false;

                                        // Un temporal (ej. resultado de STRLEN) no se conoce en compilacion: se acepta
                                        if (esTemporalNombre(valorFinalDimension)) {
                                            enRango = true;
                                        } else {
                                            int numToken = -1;
                                            try { numToken = Integer.parseInt(tokenEvaluado[0].toString()); } catch (Exception e) {}

                                            if (numToken <= -61 && numToken >= -64) { // Identificadores numéricos
                                                enRango = true;
                                            } else if (numToken == -57 || numToken == -54 || numToken == -55 || numToken == -56) { // Constantes numéricas
                                                try {
                                                    int valorConstante = Integer.parseInt(valorFinalDimension);
                                                    if (valorConstante >= 0 && valorConstante < limiteMaximo) enRango = true;
                                                } catch(Exception ignored) { enRango = true; }
                                            } else {
                                                enRango = true;
                                            }
                                        }

                                        if (!enRango) {
                                            String descFalloRango = "Índice fuera de rango: se usó '" + valorFinalDimension + "'";
                                            listaRegistroReglas.add(new Object[]{tokenLinea, cveRegla5, descRegla5, "Error", valorFinalDimension, ambitoActual, topeRango});
                                            listaErroresSemantico2.add(new Object[]{548, descFalloRango, arregloEnUso.id, "Semantico 2", tokenLinea});
                                        } else {
                                            listaRegistroReglas.add(new Object[]{tokenLinea, cveRegla5, descRegla5, "Acept", valorFinalDimension, ambitoActual, topeRango});
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Texto de la dimension (para reconstruir "#DArr[1]") y limpieza para la siguiente
                    if (!expresionActual.isEmpty()) c.dimsTxt.add(textoDe(expresionActual));
                    expresionActual.clear();
                    valorDerechoStr = "";
                }
                pilaSintactica.pop();
                continue;
            }

            // =====================================================================
            // Acción 821: Reiniciar operación después de la coma (Arreglos y Funciones)
            // =====================================================================
            if (tope.equals("821")){
                expresionActual.clear();
                valorDerechoStr = "";
                leyendoOperacion = true;
                pilaSintactica.pop();
                continue;
            }

            // =====================================================================
            // Acción 822: Cierra el Arreglo
            // Restaura la expresion de afuera y le agrega el arreglo como UN token.
            // =====================================================================
            if (tope.equals("822")){
                nivelDeCorchetes--;
                CtxArreglo c = (!pilaCtx.isEmpty() && pilaCtx.peek() instanceof CtxArreglo) ? (CtxArreglo) pilaCtx.pop() : null;

                if (c != null) {
                    if (c.arr != null && c.arr.dimensionArr != null && c.dims < c.arr.dimensionArr) {
                        String cveRegla3 = "1030";
                        String descRegla3 = "CANT. DIMENSIONES";
                        int ambitoActual = pilaContadorAmbito.isEmpty() ? 0 : pilaContadorAmbito.peek();
                        listaRegistroReglas.add(new Object[]{tokenActual[2], cveRegla3, descRegla3, "Error", c.arr.id, ambitoActual, "Dim " + c.arr.dimensionArr});
                        listaErroresSemantico2.add(new Object[]{546, descRegla3, c.arr.id, "Semantico 2", tokenActual[2]});
                    }

                    String texto = c.id + "[" + String.join(", ", c.dimsTxt) + "]";
                    // Arreglo al inicio de una sentencia (lado izquierdo): el destino es el arreglo,
                    // no el ultimo id leido dentro del indice (ej. #DL[#DFun(1)] = 1)
                    if (!c.leyendoPrev) { ultimoIdVisto = c.id; ladoIzquierdoArreglo = true; }
                    expresionActual.clear();
                    expresionActual.addAll(c.exprPrevia);
                    leyendoOperacion = c.leyendoPrev;
                    valorDerechoStr = c.valorDerPrev;
                    if (leyendoOperacion) {
                        // Se conserva el codigo y lexema del id para que Semantica 1 lo trate igual que antes
                        expresionActual.add(new Object[]{c.codigoId, c.id, c.linea, texto, "ARR"});
                        if (expresionActual.size() == 1) valorDerechoStr = c.id;
                    }
                }

                boolean quedaArreglo = false;
                for (Object o : pilaCtx) if (o instanceof CtxArreglo) { quedaArreglo = true; break; }
                if (!quedaArreglo) vieneDeArreglo = false;

                pilaSintactica.pop();
                continue;
            }

            if (tope.equals("823")){
                estructuraEnUso = (String) tokenActual[1];
                pilaSintactica.pop();
                continue;
            }

            if (tope.equals("824")){
                leyendoOperacion = false;

                if (!expresionActual.isEmpty()) {
                    String valorFinal = "";
                    Object[] tokenEvaluado = null;

                    // 1. Resolvemos la expresión del switch (Igual que en los arreglos)
                    if (expresionActual.size() == 1) {
                        tokenEvaluado = expresionActual.get(0);
                        valorFinal = (String) tokenEvaluado[1];
                    } else {
                        List<Object[]> prefijaDer = convertirAPrefijo(expresionActual);
                        indiceGlobalPrefijo = 0;
                        int indexInicioCuadruplos = listaCuadruplos.size();
                        generarCuadruplosRecursivo(prefijaDer);

                        if (listaCuadruplos.size() > indexInicioCuadruplos) {
                            String[] ultimoCuad = listaCuadruplos.get(listaCuadruplos.size() - 1);
                            if (ultimoCuad.length > 3 && ultimoCuad[3] != null && ultimoCuad[3].startsWith("T")) {
                                valorFinal = ultimoCuad[3];
                            }
                        }
                        if (valorFinal.isEmpty() && valorDerechoStr != null && !valorDerechoStr.isEmpty()) {
                            valorFinal = valorDerechoStr;
                        }
                        
                        int cod = 0; // Valor por defecto (para Variant o errores)
                        if (valorFinal.startsWith("TBI")) {
                            cod = -61; // Binario
                        } else if (valorFinal.startsWith("TD")) {
                            cod = -62; // Decimal
                        } else if (valorFinal.startsWith("TO")) {
                            cod = -63; // Octal
                        } else if (valorFinal.startsWith("TH")) {
                            cod = -64; // Hexadecimal
                        } else if (valorFinal.startsWith("TR")) {
                            cod = -65; // Real
                        } else if (valorFinal.startsWith("TE")) {
                            cod = -66; // Exponencial
                        } else if (valorFinal.startsWith("TC")) {
                            cod = -60; // Cadena
                        } else if (valorFinal.startsWith("TB")) {
                            cod = -67; // Booleano
                        } else if (valorFinal.startsWith("TV")) {
                            cod = 0;   // Variant (si tienes un código para Variant, ponlo aquí)
                        }
                        tokenEvaluado = new Object[]{cod, valorFinal, tokenActual[2]};
                    }

                    lexemaSwitchActual = valorFinal;

                    // 2. Determinamos su tipo exacto
                    tipoSwitchActual = getTipo(tokenEvaluado);
                // if (tipoSwitchActual.equals("Bin") || tipoSwitchActual.equals("Oct") || tipoSwitchActual.equals("Hex")) {
                //     tipoSwitchActual = "Dec"; // Normalizamos las bases numéricas a Decimal (Enteros)
                // } 
                    if (tipoSwitchActual.isEmpty()) {
                        Identificador id = buscarEnTabla(valorFinal);
                        if (id != null) {
                            tipoSwitchActual = id.tipo;
                        }
                    }

                    String descRegla6 = "SWITCH DEC O CAD";
                    int ambitoActual = pilaContadorAmbito.peek();
                    if (tipoSwitchActual.equals("Dec") || tipoSwitchActual.equals("Cadena") || tipoSwitchActual.equals("Variant")) {
                        //System.out.println("bien = Switch válido de tipo: " + tipoSwitchActual);
                        listaRegistroReglas.add(new Object[]{
                            tokenEvaluado[2], "1060", descRegla6, 
                            "Acept", valorFinal,ambitoActual,"id"
                        });
                    } else {
                        //System.out.println("error Incompatibilidad: " + descRegla6 + " (Se evaluó: " + tipoSwitchActual + ")");
                        listaRegistroReglas.add(new Object[]{
                            tokenEvaluado[2], "1060", descRegla6, 
                            "Error", valorFinal,ambitoActual,"id"
                        });
                        Object[] datosError = {550, descRegla6, valorFinal, "Semantico 2", tokenEvaluado[2]};
                        listaErroresSemantico2.add(datosError);
                        
                        // Lo forzamos a Error para que el primer case intente inferir un tipo válido
                        tipoSwitchActual = "Error";
                    }

                    expresionActual.clear();
                    valorDerechoStr = "";
                }

                pilaSintactica.pop();
                continue;
            }

            if (tope.equals("825")){
                leyendoOperacion = false;
                if (!expresionActual.isEmpty()) {
                    String valorFinalCase = "";
                    Object[] tokenEvaluadoCase = null;

                    // 1. Resolvemos la expresión del Case
                    if (expresionActual.size() == 1) {
                        tokenEvaluadoCase = expresionActual.get(0);
                        valorFinalCase = (String) tokenEvaluadoCase[1];
                    } else {
                        List<Object[]> prefijaDer = convertirAPrefijo(expresionActual);
                        indiceGlobalPrefijo = 0;
                        int indexInicioCuadruplos = listaCuadruplos.size();
                        generarCuadruplosRecursivo(prefijaDer);

                        if (listaCuadruplos.size() > indexInicioCuadruplos) {
                            String[] ultimoCuad = listaCuadruplos.get(listaCuadruplos.size() - 1);
                            if (ultimoCuad.length > 3 && ultimoCuad[3] != null && ultimoCuad[3].startsWith("T")) {
                                valorFinalCase = ultimoCuad[3];
                            }
                        }
                        if (valorFinalCase.isEmpty() && valorDerechoStr != null && !valorDerechoStr.isEmpty()) {
                            valorFinalCase = valorDerechoStr;
                        }
                        int cod = 0; // Valor por defecto (para Variant o errores)

                        if (valorFinalCase.startsWith("TBI")) {
                            cod = -61; // Binario
                        } else if (valorFinalCase.startsWith("TD")) {
                            cod = -62; // Decimal
                        } else if (valorFinalCase.startsWith("TO")) {
                            cod = -63; // Octal
                        } else if (valorFinalCase.startsWith("TH")) {
                            cod = -64; // Hexadecimal
                        } else if (valorFinalCase.startsWith("TR")) {
                            cod = -65; // Real
                        } else if (valorFinalCase.startsWith("TE")) {
                            cod = -66; // Exponencial
                        } else if (valorFinalCase.startsWith("TC")) {
                            cod = -60; // Cadena
                        } else if (valorFinalCase.startsWith("TB")) {
                            cod = -67; // Booleano
                        } else if (valorFinalCase.startsWith("TV")) {
                            cod = 0;   // Variant (si tienes un código para Variant, ponlo aquí)
                        }
                        tokenEvaluadoCase = new Object[]{cod, valorFinalCase, tokenActual[2]};
                    }

                    // 2. Determinamos el tipo del Case
                    String tipoCase = getTipo(tokenEvaluadoCase);
                    // if (tipoCase.equals("Bin") || tipoCase.equals("Oct") || tipoCase.equals("Hex")) {
                    //     tipoCase = "Dec"; 
                    // } 
                    if (tipoCase.isEmpty()) {
                        Identificador id = buscarEnTabla(valorFinalCase);
                        if (id != null) {
                            tipoCase = id.tipo;
                        }
                    }

                    // 3. COMPARAMOS Y APLICAMOS REGLA 1061 (CASE)[cite: 8]
                    String descRegla6 = "MISMO TIPO EXP CASO";
                    int ambitoActual = pilaContadorAmbito.peek();

                    // CASO A: El switch es Variant o venía con Error
                    if (tipoSwitchActual.equals("Variant") || tipoSwitchActual.equals("Error")) {
                        
                        if (tipoCase.equals("Dec") || tipoCase.equals("Cadena")) {
                            // INFERENCIA: Tomamos este tipo como el válido para todo el resto del switch
                            tipoSwitchActual = tipoCase;
                            
                            //System.out.println("bien = El switch se ha adaptado al tipo del caso: " + tipoSwitchActual);
                            listaRegistroReglas.add(new Object[]{
                                tokenEvaluadoCase[2], "1061", descRegla6, 
                                "Acept", valorFinalCase,ambitoActual,tipoSwitchActual
                            });
                        } else {
                            // El primer caso tampoco es válido
                            String errorMsg = "El caso '" + valorFinalCase + "' es inválido. Debe ser Dec o Cadena.";
                            //System.out.println("error " + errorMsg);
                            listaRegistroReglas.add(new Object[]{
                                tokenEvaluadoCase[2], "1061", descRegla6, 
                                "Error", valorFinalCase,ambitoActual,tipoSwitchActual
                            });
                            listaErroresSemantico2.add(new Object[]{551, errorMsg,valorFinalCase, "Semantico 2", tokenEvaluadoCase[2]});
                        }
                        
                    } 
                    // CASO B: El switch ya tiene un tipo definido (Dec o Cadena)
                    else {
                        if (tipoCase.equals(tipoSwitchActual)) {
                            // CUMPLE
                            //System.out.println("bien = El caso coincide con el tipo del switch (" + tipoSwitchActual + ")");
                            listaRegistroReglas.add(new Object[]{
                                tokenEvaluadoCase[2], "1061", descRegla6, 
                                "Acept", valorFinalCase,ambitoActual,tipoSwitchActual
                            });
                        } else {
                            // FALLA
                            String errorMsg = "Incompatibilidad: El switch espera tipo '" + tipoSwitchActual + "', pero el caso es '" + tipoCase + "'";
                            //System.out.println("error " + errorMsg);
                            listaRegistroReglas.add(new Object[]{
                                tokenEvaluadoCase[2], "1061", descRegla6, 
                                "Error", valorFinalCase,ambitoActual,tipoSwitchActual
                            });
                            listaErroresSemantico2.add(new Object[]{551, errorMsg, valorFinalCase, "Semantico 2", tokenEvaluadoCase[2]});
                        }
                    }
                    expresionActual.clear();
                    valorDerechoStr = "";
                }
                pilaSintactica.pop();
                continue;
            }

            if (tope.equals("826")){
                String lexemaVar = ultimoIdVisto; // Rescatamos el nombre de la variable
                Identificador idVar = buscarEnTabla(lexemaVar);
                
                String cveRegla7 = "1070";
                String descRegla7 = "UNARIO CON ENTERO";
                int ambitoActual = pilaContadorAmbito.peek(); 
                
                boolean esEntero = false;
                
                // Verificamos si la variable existe y si es Decimal (Entero) o Variant
                //CHECAR SI OTROS TIPOS MAS
                if (idVar != null) {
                    if (idVar.tipo.startsWith("Dec")) {
                        esEntero = true;
                    }
                }

                else{
                    if (lexemaVar.startsWith("#D")) {
                        esEntero = true;
                    }
                }

                if (!esEntero) {
                    // FALLA REGLA 7
                    String tipoActual = (idVar != null) ? idVar.tipo : "Desconocido";
                    String errorMsg = "La variable '" + lexemaVar + "' es de tipo '" + tipoActual + "'";
                    
                    //System.out.println("error " + descRegla7 + ". " + errorMsg);

                    listaRegistroReglas.add(new Object[]{
                        tokenActual[2], 
                        cveRegla7, 
                        descRegla7, 
                        "Error", 
                        "++ / -- " + lexemaVar, 
                        ambitoActual,
                        "TD"
                    });

                    // Disparamos error semántico 552 (Ajusta el código de error si es necesario)
                    Object[] datosError = {552, descRegla7, lexemaVar, "Semantico 2 ", tokenActual[2]};
                    listaErroresSemantico2.add(datosError);

                } else {
                    // CUMPLE REGLA 7
                    //System.out.println("bien = Operador unario válido para variable entera '" + lexemaVar + "'");

                    listaRegistroReglas.add(new Object[]{
                        tokenActual[2], 
                        cveRegla7, 
                        descRegla7, 
                        "Acept", 
                        "++ / -- " + lexemaVar, 
                        ambitoActual,
                        "TD"
                    });
                }
                pilaSintactica.pop();
                continue;
            }

            if (tope.equals("827") || tope.equals("828")){
                leyendoOperacion = false;

                if (!expresionActual.isEmpty()) {
                    String valorFinal = "";
                    Object[] tokenEvaluado = null;

                    // 1. Resolvemos la expresión dentro de los paréntesis
                    if (expresionActual.size() == 1) {
                        tokenEvaluado = expresionActual.get(0);
                        valorFinal = (String) tokenEvaluado[1];
                    } else {
                        List<Object[]> prefijaDer = convertirAPrefijo(expresionActual);
                        indiceGlobalPrefijo = 0;
                        int indexInicioCuadruplos = listaCuadruplos.size();
                        generarCuadruplosRecursivo(prefijaDer);

                        if (listaCuadruplos.size() > indexInicioCuadruplos) {
                            String[] ultimoCuad = listaCuadruplos.get(listaCuadruplos.size() - 1);
                            if (ultimoCuad.length > 3 && ultimoCuad[3] != null && ultimoCuad[3].startsWith("T")) {
                                valorFinal = ultimoCuad[3];
                            }
                        }
                        if (valorFinal.isEmpty() && valorDerechoStr != null && !valorDerechoStr.isEmpty()) {
                            valorFinal = valorDerechoStr;
                        }
                        
                        // Asignamos código de temporal para getTipo()
                        int cod = 0;
                        if (valorFinal.startsWith("TBI")) cod = -61;
                        else if (valorFinal.startsWith("TD")) cod = -62;
                        else if (valorFinal.startsWith("TO")) cod = -63;
                        else if (valorFinal.startsWith("TH")) cod = -64;
                        else if (valorFinal.startsWith("TR")) cod = -65;
                        else if (valorFinal.startsWith("TC")) cod = -60;
                        else if (valorFinal.startsWith("TB")) cod = -67;
                        
                        tokenEvaluado = new Object[]{cod, valorFinal, tokenActual[2]};
                    }

                    // 2. Determinamos su tipo exacto
                    String tipoCalculado = getTipo(tokenEvaluado);
                    if (tipoCalculado.isEmpty()) {
                        Identificador id = buscarEnTabla(valorFinal);
                        if (id != null) {
                            // Adaptamos al nombre que usas en tu tabla de símbolos
                            if (id.tipo.equals("Decimal") || id.tipo.equals("Variant")) tipoCalculado = "Dec";
                            else if (id.tipo.equals("Booleana")) tipoCalculado = "Boolean";
                            else tipoCalculado = id.tipo; // "Real", "Cadena", etc.
                        }
                    }

                    int ambitoActual = pilaContadorAmbito.peek(); 
                    String cveRegla7 = "1070";
                    String descRegla7 = "UNARIO CON ENTERO";
                    // ============================================================
                    // VALIDACIÓN PARA '!' (Acción 827 - Debe ser Booleano)
                    // ============================================================
                    if (tope.equals("827")) {
                        
                        if (tipoCalculado.equals("Boolean")) {
                            //System.out.println("bien = Operador '!' válido para: " + valorFinal);
                            listaRegistroReglas.add(new Object[]{tokenEvaluado[2], cveRegla7, descRegla7, "Acept", "! (" + valorFinal + ")", ambitoActual,"TB"});
                        } else {
                            String errorMsg = "Se esperaba tipo Booleano, pero la expresión es '" + tipoCalculado + "'";
                            //System.out.println("error Incompatibilidad: " + descRegla7 + ". " + errorMsg);
                            listaRegistroReglas.add(new Object[]{tokenEvaluado[2], cveRegla7, descRegla7, "Error", "! (" + valorFinal + ")", ambitoActual,"TB"});
                            listaErroresSemantico2.add(new Object[]{552, descRegla7, valorFinal, "Semantico 2", tokenEvaluado[2]});
                        }
                    }
                    
                    // ============================================================
                    // VALIDACIÓN PARA '~' (Acción 828 - Debe ser Entero)
                    // ============================================================
                    else if (tope.equals("828")) {
                        
                        if (tipoCalculado.equals("Dec") || tipoCalculado.equals("Bin") || tipoCalculado.equals("Oct") || tipoCalculado.equals("Hex")) {
                            //System.out.println("bien = Operador '~' válido para tipo entero: " + valorFinal);
                            listaRegistroReglas.add(new Object[]{tokenEvaluado[2], cveRegla7, descRegla7, "Acept", "~ (" + valorFinal + ")", ambitoActual,"TD"});
                        } else {
                            String errorMsg = "Se esperaba tipo Entero, pero la expresión es '" + tipoCalculado + "'";
                            //System.out.println("error Incompatibilidad: " + descRegla7 + ". " + errorMsg);
                            listaRegistroReglas.add(new Object[]{tokenEvaluado[2], cveRegla7, descRegla7, "Error", "~ (" + valorFinal + ")", ambitoActual,"TD"});
                            listaErroresSemantico2.add(new Object[]{552, descRegla7, valorFinal, "Semantico 2", tokenEvaluado[2]});
                        }
                    }

                    expresionActual.clear();
                    valorDerechoStr = "";
                }
                
                pilaSintactica.pop();
                continue;   
            }
            if (tope.equals("829")){
                leyendoOperacion = false;
                if (!expresionActual.isEmpty()) {
                    idIteradorFor = (String) expresionActual.get(0)[1]; // Guardamos la variable (ej. #Dcar)
                    expresionActual.clear();
                }
                leyendoOperacion = true; // Encendemos para leer la colección
                pilaSintactica.pop();
                continue;
            }

            if (tope.equals("830")) {
                leyendoOperacion = false;
                
                // =================================================================
                // 1. RESCATE MÁGICO DEL VALOR 2 (Evita que ignore arreglos con índice)
                // =================================================================
                String idColeccion = "";
                if (!expresionActual.isEmpty()) {
                    idColeccion = (String) expresionActual.get(0)[1]; 
                } else {
                    idColeccion = idIzquierdoStr; // Rescate de emergencia para #DLista[1]
                }

                // 2. Ahora sí, si logramos atrapar el Valor 2, evaluamos las reglas
                if (!idColeccion.isEmpty()) { 
                    String lexema = idIteradorFor + " : " + idColeccion;
                    int ambito = pilaContadorAmbito.isEmpty() ? 0 : pilaContadorAmbito.peek();

                    // Buscamos ambos identificadores en la tabla
                    Identificador objIterador = buscarEnTabla(idIteradorFor); // Valor 1
                    Identificador objColeccion = buscarEnTabla(idColeccion);  // Valor 2

                    // Obtenemos los tipos y clases (con respaldos)
                    String tipo1 = "";
                    if (objIterador != null) tipo1 = objIterador.tipo;
                    else if (idIteradorFor.startsWith("#D")) tipo1 = "Decimal";
                    else if (idIteradorFor.startsWith("TV")) tipo1 = "Variant";
                    
                    String tipo2 = "";
                    String clase2 = "VAR";
                    if (objColeccion != null) {
                        tipo2 = objColeccion.tipo;
                        clase2 = objColeccion.clase != null ? objColeccion.clase : "VAR";
                    } else if (idColeccion.startsWith("TV")) {
                        tipo2 = "Variant";
                    } else if (idColeccion.startsWith("@C")) {
                        tipo2 = "Cadena";
                    }

                    // Normalizamos
                    if (tipo1.equals("Dec")) tipo1 = "Decimal";
                    if (tipo2.equals("Dec")) tipo2 = "Decimal";
                    if (tipo1.isEmpty()) tipo1 = "Variant";
                    if (tipo2.isEmpty()) tipo2 = "Variant";

                    // ==========================================================
                    // EVALUACIÓN DE LAS REGLAS 1083 Y 1084 DEL FOREACH
                    // ==========================================================

                    // Un resultado de funcion, null o una constante no se pueden recorrer (regla 1084)
                    boolean noIterable = idColeccion.equals("null")
                        || (esTemporalNombre(idColeccion) && !idColeccion.startsWith("TV"))
                        || idColeccion.matches("^[0-9\"'].*|^(true|false)$|^x[bol].*");
                    if (noIterable) {
                        String errorMsg = "Regla 1084: '" + idColeccion + "' no es un arreglo";
                        listaRegistroReglas.add(new Object[]{tokenActual[2], "1084", "Manejo de arreglos", "Error", lexema, ambito});
                        listaErroresSemantico2.add(new Object[]{554, errorMsg, lexema, "Semantico 2", tokenActual[2]});
                    }
                    else if (tipo1.equals("Variant") && tipo2.equals("Variant")) {
                        //System.out.println("bien = Foreach válido (Ambos son Variant)");
                        listaRegistroReglas.add(new Object[]{tokenActual[2], "1083/1084", "Manejo de cadenas/arreglos", "Acept", lexema, ambito});
                    }
                    else if (tipo2.equals("Variant")) {
                        //System.out.println("bien = Foreach válido (Valor 2 es Variant, se infiere arreglo de tipo " + tipo1 + ")");
                        listaRegistroReglas.add(new Object[]{tokenActual[2], "1084", "Manejo de arreglos", "Acept", lexema, ambito});
                    }
                    // AQUI EVALUAMOS ARR ANTES QUE CADENA (¡Bien hecho!)
                    else if (clase2.equals("ARR")) {
                        if (tipo1.equals(tipo2)) { 
                            //System.out.println("bien = Foreach válido de Arreglo (tipos coinciden: " + tipo1 + ")");
                            listaRegistroReglas.add(new Object[]{tokenActual[2], "1084", "Manejo de arreglos", "Acept", lexema, ambito});
                        } else {
                            String errorMsg = "Regla 1084: El valor 1 debe ser del mismo tipo que el arreglo. Valor 1: " + tipo1 + ", Valor 2: " + tipo2;
                            //System.out.println("error " + errorMsg);
                            listaRegistroReglas.add(new Object[]{tokenActual[2], "1084", "Manejo de arreglos", "Error", lexema, ambito});
                            listaErroresSemantico2.add(new Object[]{554, errorMsg, lexema, "Semantico 2", tokenActual[2]});
                        }
                    }
                    else if (tipo2.equals("Cadena")) {
                        if (tipo1.equals("Decimal")) { 
                            //System.out.println("bien = Foreach válido de Cadena con iterador Entero");
                            listaRegistroReglas.add(new Object[]{tokenActual[2], "1083", "Manejo de cadenas", "Acept", lexema, ambito});
                        } else {
                            String errorMsg = "Regla 1083: Si el valor 2 es Cadena, el valor 1 debe ser Entero (#D). Se encontró: " + tipo1;
                           // System.out.println("error " + errorMsg);
                            listaRegistroReglas.add(new Object[]{tokenActual[2], "1083", "Manejo de cadenas", "Error", lexema, ambito});
                            listaErroresSemantico2.add(new Object[]{553, errorMsg, lexema, "Semantico 2", tokenActual[2]});
                        }
                    }
                    else {
                        String errorMsg = "El valor a iterar '" + idColeccion + "' no es una Cadena, un Arreglo, ni Variant";
                    //    System.out.println("error " + errorMsg);
                        listaRegistroReglas.add(new Object[]{tokenActual[2], "1083/1084", "Manejo de cadenas/arreglos", "Error", lexema, ambito});
                        listaErroresSemantico2.add(new Object[]{555, errorMsg, lexema, "Semantico 2", tokenActual[2]});
                    }

                    expresionActual.clear();
                }
                pilaSintactica.pop();
                continue;
            }

            // =====================================================================
            // FOR CLÁSICO: Acción 831 (Se activa al leer el primer ';') -> Valida Inicialización
            // =====================================================================
            if (tope.equals("831")) {
                leyendoOperacion = false;
                String cve = "1080";
                String desc = "FOR INICIALIZACION";
                int ambito = pilaContadorAmbito.peek(); 
                expresionActual.removeIf(tok -> tok[1].toString().equals(";"));
                if (huboAsignacionFor) {
                    // La 811 y 812 ya procesaron la asignación real. Aquí solo armamos el texto.
                    // Rescatamos el valor derecho (ej. "0") de donde haya quedado guardado
                    String valorDer = (valorDerechoStr != null && !valorDerechoStr.isEmpty()) ? valorDerechoStr : "";
                    if (valorDer.isEmpty() && !expresionActual.isEmpty()) {
                        valorDer = (String) expresionActual.get(0)[1];
                    }
                    
                    String lexemaInit = lexemaAsignacionFor + " " + valorDer; 
                  //  System.out.println("bien = Inicialización de for válida: " + lexemaInit);
                    listaRegistroReglas.add(new Object[]{tokenActual[2], cve, desc, "Acept", lexemaInit.trim(), ambito,"id"});
                    
                } else {
                    // Si nunca pasó por la 811, no hubo '='
                    String errorMsg = "La primera parte del for debe ser una asignación (ej. x = 0)";
                  //  System.out.println("error " + errorMsg);
                    listaRegistroReglas.add(new Object[]{tokenActual[2], cve, desc, "Error", "Inicialización inválida", ambito,"id"});
                    listaErroresSemantico2.add(new Object[]{557, errorMsg, "for(...)", "Semantico 2", tokenActual[2]});
                }
                leyendoOperacion = true; // Encendemos para leer la condición booleana
                expresionActual.clear();
                valorDerechoStr = "";
                huboAsignacionFor = false;
                lexemaAsignacionFor = "";
                parteForClasico = 2;
                pilaSintactica.pop();
                continue;
            }

            // =====================================================================
            // FOR CLÁSICO: Acción 832 (Se activa al leer el segundo ';') -> Valida Condición
            // =====================================================================
            if (tope.equals("832")) {
                leyendoOperacion = false;
                expresionActual.removeIf(tok -> tok[1].toString().equals(";"));
                if (!expresionActual.isEmpty()) {
                    String valorFinal = "";
                    Object[] tokenEvaluado = null;
                    // Resolvemos la expresión matemática/lógica
                    if (expresionActual.size() == 1) {
                        tokenEvaluado = expresionActual.get(0);
                        valorFinal = (String) tokenEvaluado[1];
                    } else {
                        List<Object[]> prefijaDer = convertirAPrefijo(expresionActual);
                        indiceGlobalPrefijo = 0;
                        int indexInicioCuadruplos = listaCuadruplos.size();
                        generarCuadruplosRecursivo(prefijaDer);

                        if (listaCuadruplos.size() > indexInicioCuadruplos) {
                            String[] ultimoCuad = listaCuadruplos.get(listaCuadruplos.size() - 1);
                            if (ultimoCuad.length > 3 && ultimoCuad[3] != null && ultimoCuad[3].startsWith("T")) {
                                valorFinal = ultimoCuad[3];
                            }
                        }
                        if (valorFinal.isEmpty() && valorDerechoStr != null && !valorDerechoStr.isEmpty()) valorFinal = valorDerechoStr;
                        
                        int cod = valorFinal.startsWith("TB") ? -67 : (valorFinal.startsWith("TD") ? -62 : 0);
                        tokenEvaluado = new Object[]{cod, valorFinal, tokenActual[2]};
                    }

                    // Reconstruimos lexema completo para el reporte
                    StringBuilder lexemaCond = new StringBuilder();
                    for (Object[] tok : expresionActual) lexemaCond.append(tok[1]).append(" ");

                    String tipoCondicion = getTipo(tokenEvaluado);
                    if (tipoCondicion.isEmpty()) {
                        Identificador id = buscarEnTabla(valorFinal);
                        if (id != null) tipoCondicion = id.tipo;
                    }

                    String cve = "1081";
                    String desc = "ID COMPARACION";
                    int ambito = pilaContadorAmbito.peek();

                    if (tipoCondicion.equals("Boolean") || tipoCondicion.equals("Booleana")) {
                      //  System.out.println("bien = Condición de for válida: " + lexemaCond);
                        listaRegistroReglas.add(new Object[]{tokenActual[2], cve, desc, "Acept", lexemaCond.toString().trim(), ambito,"TB"});
                    } else {
                        String errorMsg = "La condición del for debe ser Booleana, se encontró: " + tipoCondicion;
                     //   System.out.println("error " + errorMsg);
                        listaRegistroReglas.add(new Object[]{tokenActual[2], cve, desc, "Error", lexemaCond.toString().trim(), ambito,"TB"});
                        listaErroresSemantico2.add(new Object[]{555, errorMsg, lexemaCond.toString().trim(), "Semantico 2", tokenActual[2]});
                    }
                    expresionActual.clear();
                }
                leyendoOperacion = true; // Encendemos para leer el incremento
                parteForClasico = 3;
                valorDerechoStr = "";   
                // NUEVO: Limpiamos por seguridad antes de leer la 3ra parte
                tokenAsignacion = null; 
                idIzquierdoStr = "";
                pilaSintactica.pop();
                continue;
            }

            //TODO VERIFICAR EL INCREMENTO QUE ESTE BIEN
            // =====================================================================
            // FOR CLÁSICO: Acción 833 (Se activa al cerrar el for)
            // =====================================================================
            if (tope.equals("833")) {
                leyendoOperacion = false;
                expresionActual.removeIf(tok -> tok[1].toString().equals(";"));
                int ambito = pilaContadorAmbito.peek();

                // ======================================================
                // RUTA 1: HUBO ASIGNACIÓN COMPUESTA 
                // (Ignoramos porque la Acción 811 ya registró la 1021 / 1022)
                // ======================================================
                if (huboAsignacionFor && (lexemaAsignacionFor.contains("+=") || lexemaAsignacionFor.contains("-=") || 
                                          lexemaAsignacionFor.contains("*=") || lexemaAsignacionFor.contains("/="))) {
                    
                 //   System.out.println("bien = Asignación compuesta en FOR procesada por reglas base");
                    
                }
                // ======================================================
                // RUTA 2: TIENE ++ O -- (Dispara Regla 1082)
                // ======================================================
                if (!expresionActual.isEmpty()) {
                    boolean tieneIncDec = false;
                    String lexemaInc = "";
                    
                    for (int j = 0; j < expresionActual.size(); j++) {
                        String lex = (String) expresionActual.get(j)[1];
                        lexemaInc += lex + " ";
                        if (lex.equals("++") || lex.equals("--")) {
                            tieneIncDec = true;
                        }
                    }

                    String cve = "1082";
                    String desc = "INCREMENTO O DECREMENTO";

                    if (tieneIncDec) {
                      //  System.out.println("bien = Incremento de for válido: " + lexemaInc);
                        listaRegistroReglas.add(new Object[]{tokenActual[2], cve, desc, "Acept", lexemaInc.trim(), ambito, "op"});
                    } else {
                        String errorMsg = "La tercera parte del for debe contener ++, -- o una asignación compuesta";
                      //  System.out.println("error " + errorMsg);
                        listaRegistroReglas.add(new Object[]{tokenActual[2], cve, desc, "Error", lexemaInc.trim(), ambito, "op"});
                        listaErroresSemantico2.add(new Object[]{556, errorMsg, lexemaInc.trim(), "Semantico 2", tokenActual[2]});
                    }
                }

                // Limpieza de banderas del ciclo
                expresionActual.clear();
                valorDerechoStr = "";
                huboAsignacionFor = false;
                lexemaAsignacionFor = "";
                tokenAsignacion = null;
                idIzquierdoStr = "";
                parteForClasico = 0;
                pilaSintactica.pop();
                continue;
            }

            // =====================================================================
            // Acción 834: Validación exclusiva para condiciones (if, while, do-while)
            // =====================================================================
            if (tope.equals("834")) {
                leyendoOperacion = false;
                
                if (!expresionActual.isEmpty()) {
                    String valorFinalParaError = "";
                    
                    // 1. Resolvemos la expresión matemática/lógica de la condición
                    boolean condicionVariant = false;
                    if (expresionActual.size() == 1) {
                        valorFinalParaError = (String) expresionActual.get(0)[1];
                        // Un id no declarado se toma como Variant (genera su temporal), igual que en las operaciones
                        Object[] tokCond = expresionActual.get(0);
                        if (etiqueta(tokCond).isEmpty() && esIdentificadorLexema(valorFinalParaError) && buscarEnTabla(valorFinalParaError) == null) {
                            contadoresTemporales[VAR]++;
                            valorFinalParaError = PREFIJOS_TEMP[VAR] + contadoresTemporales[VAR];
                            condicionVariant = true;
                        }
                    } else {
                        List<Object[]> prefijaDer = convertirAPrefijo(expresionActual);
                        indiceGlobalPrefijo = 0;
                        int indexInicio = listaCuadruplos.size();
                        generarCuadruplosRecursivo(prefijaDer);
                        
                        if (listaCuadruplos.size() > indexInicio) {
                            String[] ultimoCuad = listaCuadruplos.get(listaCuadruplos.size() - 1);
                            if (ultimoCuad.length > 3 && ultimoCuad[3] != null && ultimoCuad[3].startsWith("T")) {
                                valorFinalParaError = ultimoCuad[3];
                            }
                        }
                        if (valorFinalParaError.isEmpty() && valorDerechoStr != null && !valorDerechoStr.isEmpty()) {
                            valorFinalParaError = valorDerechoStr;
                        }
                    }

                    // 2. Determinamos si el resultado es Lógico
                    boolean esLogico = condicionVariant;
                    if (valorFinalParaError.startsWith("TB")) {
                        esLogico = true;
                    } else if (valorFinalParaError.equals("true") || valorFinalParaError.equals("false")) {
                        esLogico = true;
                    } else {
                        Identificador idVar = buscarEnTabla(valorFinalParaError);
                        if (idVar != null && (idVar.tipo.equals("Boolean") || idVar.tipo.equals("TBool") || idVar.tipo.equals("Booleana"))) {
                            esLogico = true;
                        }
                    }

                    // 3. Asignamos CVE según la estructura
                    String cveRegla = "";
                    if (estructuraEnUso.equals("if")) cveRegla = "1010";
                    else if (estructuraEnUso.equals("elseif")) cveRegla = "1011";
                    else if (estructuraEnUso.equals("while")) cveRegla = "1012";
                    else if (estructuraEnUso.equals("do")) cveRegla = "1013";
                    
                 //   System.out.println("LA ESTRUCTURA ES: " + estructuraEnUso);
                    String descRegla = "CONDICION ES BOOL";
                    int ambitoActual = pilaContadorAmbito.isEmpty() ? 0 : pilaContadorAmbito.peek();

                    // 4. Validamos la Regla 1
                    if (!esLogico) {
                    //    System.out.println("error Incompatibilidad de tipos: " + descRegla + ". " + valorFinalParaError);
                        listaRegistroReglas.add(new Object[]{tokenActual[2], cveRegla, descRegla, "Error", valorFinalParaError, ambitoActual,"TB"});

                        Object[] error = {549, descRegla, valorFinalParaError, "Semantico 2", tokenActual[2]};
                        listaErroresSemantico2.add(error);
                    } else {
                      //  System.out.println("bien = Condición lógica válida: " + valorFinalParaError);
                        listaRegistroReglas.add(new Object[]{tokenActual[2], cveRegla, descRegla, "Acept", valorFinalParaError, ambitoActual,"TB"});
                    }

                    // 5. Limpieza profunda obligatoria
                    expresionActual.clear();
                    valorDerechoStr = "";
                }
                
                pilaSintactica.pop();
                continue;
            }
            if (tope.equals("835")){
                idMetodoActual = ultimoIdVisto;
                lineaMetodoActual = tokenActual[2];
                char inicial = idMetodoActual.charAt(0);
                // El 802 ya abrio el ambito de la funcion; el ID de la funcion pertenece al ambito de afuera
                int ambitoActual = pilaContadorAmbito.size() > 1 ? pilaContadorAmbito.get(pilaContadorAmbito.size() - 2) : 0;

                // Si empieza con símbolo, es Función (Regla 12)
                if (inicial == '#' || inicial == '@' || inicial == '$' || inicial == '%' || inicial == '¡' || inicial == '¿') {
                    enFuncion = true;
                    enProcedimiento = false;
                    
                    // Usamos startsWith para detectar los numéricos y asignar el string exacto
                    if (idMetodoActual.startsWith("#B")) tipoMetodoActual = "Bin";
                    else if (idMetodoActual.startsWith("#D")) tipoMetodoActual = "Dec";
                    else if (idMetodoActual.startsWith("#O")) tipoMetodoActual = "Oct";
                    else if (idMetodoActual.startsWith("#X")) tipoMetodoActual = "Hex";
                    
                    // Asignación del resto de tipos basándose en tu getTipo()
                    else if (idMetodoActual.startsWith("@")) tipoMetodoActual = "Cadena";
                    else if (idMetodoActual.startsWith("$")) tipoMetodoActual = "Real";
                    else if (idMetodoActual.startsWith("%")) tipoMetodoActual = "Variant";
                    else if (idMetodoActual.startsWith("¡")) tipoMetodoActual = "Boolean";
                    else if (idMetodoActual.startsWith("¿")) tipoMetodoActual = "exp";
                    
                 //   System.out.println("bien = Es clase función: " + idMetodoActual + " (Tipo esperado: " + tipoMetodoActual + ")");
                    // Una def duplicada no se vuelve a registrar como clase funcion (como Abby)
                    if (!ultimoIdDuplicado) {
                        listaRegistroReglas.add(new Object[]{tokenActual[2], "1120", "El ID debe ser clase función", "Acept", idMetodoActual, ambitoActual,"func"});
                    }
}
                // Si empieza con letra, es Procedimiento (Regla 11)[cite: 11]
                else {
                    enProcedimiento = true;
                    enFuncion = false;
                    tipoMetodoActual = "Void";
                    
                  //  System.out.println("bien = Es clase procedimiento: " + idMetodoActual);
                    listaRegistroReglas.add(new Object[]{tokenActual[2], "1110", "ID clase procedimiento", "Acept", idMetodoActual, ambitoActual,"proc"});
                }
                
                encontroReturn = false;

                CtxMetodo m = new CtxMetodo();
                m.id = idMetodoActual;
                m.tipo = tipoMetodoActual;
                m.esFuncion = enFuncion;
                m.linea = lineaMetodoActual;
                m.profundidad = pilaContadorAmbito.size();
                pilaMetodos.push(m);

                pilaSintactica.pop();
                continue;
            }

            // Acción 836: Prepara la lectura de la expresión del return
            if (tope.equals("836")){
                expresionActual.clear();
                leyendoOperacion = true;
                pilaSintactica.pop();
                continue;
            }

            // Acción 837: Evalúa el tipo de retorno y valida Regla 14 y 1131
            if (tope.equals("837")){
                leyendoOperacion = false;
                int ambitoActual = pilaContadorAmbito.isEmpty() ? 0 : pilaContadorAmbito.peek();
                lineaDelReturn = tokenActual[2].toString();
                // Si es Procedimiento y tiene return, falla la Regla 14[cite: 13]
                if (enProcedimiento) {
                    encontroReturn = true;
                    if (!pilaMetodos.isEmpty()) pilaMetodos.peek().encontroReturn = true;
                    String valorRet = expresionActual.isEmpty() ? "return"
                                    : (expresionActual.size() == 1 ? textoReporte(expresionActual.get(0)) : textoDe(expresionActual));
                    String errorMsg = "Un procedimiento no debe de tener retorno";
                    listaRegistroReglas.add(new Object[]{tokenActual[2], "1140", "NO RETURN DE PROCEDIMIENTO", "Error", valorRet, ambitoActual,"TVoid"});
                    listaErroresSemantico2.add(new Object[]{559, errorMsg, "return " + valorRet, "Semantico 2", tokenActual[2]});
                } 
                // Si es Función, validamos que el tipo coincida (Regla 1131)[cite: 13]
                else if (enFuncion) {
                    boolean primerReturn = !encontroReturn;
                    encontroReturn = true;
                    if (!pilaMetodos.isEmpty()) pilaMetodos.peek().encontroReturn = true;
                    
                    String valorFinal = "";
                    Object[] tokenEvaluado = null;
                    
                    if (!expresionActual.isEmpty()) {
                        if (expresionActual.size() == 1) {
                            tokenEvaluado = expresionActual.get(0);
                            valorFinal = (String) tokenEvaluado[1];
                        } else {
                            List<Object[]> prefijaDer = convertirAPrefijo(expresionActual);
                            indiceGlobalPrefijo = 0;
                            int indexInicio = listaCuadruplos.size();
                            generarCuadruplosRecursivo(prefijaDer);
                            
                            if (listaCuadruplos.size() > indexInicio) {
                                String[] ultimoCuad = listaCuadruplos.get(listaCuadruplos.size() - 1);
                                if (ultimoCuad.length > 3 && ultimoCuad[3] != null && ultimoCuad[3].startsWith("T")) {
                                    valorFinal = ultimoCuad[3];
                                }
                            }
                            if (valorFinal.isEmpty() && valorDerechoStr != null && !valorDerechoStr.isEmpty()) valorFinal = valorDerechoStr;
                            int cod = 0; // Por defecto

                            // 1. Asignamos el código numérico según el prefijo del temporal[cite: 14]
                            if (valorFinal.startsWith("TBI")) { 
                                cod = -61; // Binario[cite: 14]
                            } else if (valorFinal.startsWith("TD")) {
                                cod = -62; // Decimal (Entero)[cite: 14]
                            } else if (valorFinal.startsWith("TO")) {
                                cod = -63; // Octal[cite: 14]
                            } else if (valorFinal.startsWith("TH")) {
                                cod = -64; // Hexadecimal[cite: 14]
                            } else if (valorFinal.startsWith("TR")) {
                                cod = -65; // Real[cite: 14]
                            } else if (valorFinal.startsWith("TE")) {
                                cod = -66; // Exponencial[cite: 14]
                            } else if (valorFinal.startsWith("TC")) {
                                cod = -60; // Cadena[cite: 14]
                            } else if (valorFinal.startsWith("TB")) { // ¡TB debe ir DESPUÉS de TBI!
                                cod = -67; // Booleano (Lógico)[cite: 14]
                            } else if (valorFinal.startsWith("TV")) {
                                cod = 0;   // Variant (Ajusta si tienes un código específico para Variant en tu léxico)
                            }

                            tokenEvaluado = new Object[]{cod, valorFinal, tokenActual[2]};
                        }
                    }
                    
                    String tipoRetorno = getTipo(tokenEvaluado);
                    if (expresionActual.size() == 1 && etiqueta(expresionActual.get(0)).equals("VOID")) {
                        tipoRetorno = "Void";      // ej. return copy(@a, @b)
                    }
                    if (tipoRetorno.isEmpty()) {
                        Identificador id = buscarEnTabla(valorFinal);
                        if (id != null) tipoRetorno = id.tipo;
                    }
                    if (tipoRetorno.isEmpty()) tipoRetorno = "Variant";

                    // Regla 1130: la funcion tiene return. Solo en el PRIMER return (antes del 1131)
                    if (primerReturn) listaRegistroReglas.add(new Object[]{tokenActual[2], "1130", "RETURN DE FUNCION", "Acept", idMetodoActual, ambitoActual, "return"});

                    String valorRetorno = (expresionActual.size() == 1) ? textoReporte(expresionActual.get(0)) : valorFinal;
                    // Regla 1131: Comparación de tipos
                    if (tipoMetodoActual.equals(tipoRetorno) || tipoMetodoActual.equals("Variant")|| tipoRetorno.equals("Variant")) {
                        listaRegistroReglas.add(new Object[]{tokenActual[2], "1131", "Tipo de retorno igual", "Acept", valorRetorno, ambitoActual, tipoConT(tipoMetodoActual)});
                    } else {
                        String errorMsg = "El tipo de retorno (" + tipoRetorno + ") no coincide con la función (" + tipoMetodoActual + ")";
                        listaRegistroReglas.add(new Object[]{tokenActual[2], "1131", "Tipo de retorno igual", "Error", valorRetorno, ambitoActual, tipoConT(tipoMetodoActual)});
                        listaErroresSemantico2.add(new Object[]{560, errorMsg, "return " + valorFinal, "Semantico 2", tokenActual[2]});
                    }
                }
                
                expresionActual.clear();
                valorDerechoStr = "";
                pilaSintactica.pop();
                continue;
            }
            // =====================================================================
            // Acción 838: Inicia la llamada a una función de usuario
            // Contexto propio en pilaCtx: #DFun(#DFun(1)) ya no pierde la llamada de afuera.
            // =====================================================================
            if (tope.equals("838")) {
                CtxLlamada c = new CtxLlamada();
                c.nombre = ultimoIdVisto;              // id justo antes del '('
                c.func = buscarEnTabla(ultimoIdVisto);
                Object[] tokId = (i >= 2) ? listaTokens.get(i - 2) : null;   // i-1 = "(" , i-2 = id
                c.codigoId = (tokId != null && String.valueOf(tokId[1]).equals(c.nombre)) ? tokId[0] : -142;
                c.linea = tokenActual[2];
                c.leyendoPrev = leyendoOperacion;
                c.valorDerPrev = valorDerechoStr;
                c.exprPrevia = c.leyendoPrev ? sinApertura(expresionActual, c.nombre, "(") : new ArrayList<>();
                pilaCtx.push(c);

                expresionActual.clear();
                leyendoOperacion = true;
                valorDerechoStr = "";
                pilaSintactica.pop();
                continue;
            }

            // =====================================================================
            // Acción 839: Evaluación de un argumento individual (Reglas 1150 y 1160)
            // =====================================================================
            if (tope.equals("839")) {

                // Solo si lo mas interno abierto es una llamada de usuario.
                // Las reglas 1150/1160 de cada argumento se guardan en la llamada y se registran
                // todas juntas al cerrar el ')' (accion 840), en el mismo orden que la prueba de referencia.
                if (!pilaCtx.isEmpty() && pilaCtx.peek() instanceof CtxLlamada) {
                    CtxLlamada c = (CtxLlamada) pilaCtx.peek();
                    c.nArgs++;
                    int contadorArgumentos = c.nArgs;
                    int ambitoActual = pilaContadorAmbito.isEmpty() ? 0 : pilaContadorAmbito.peek();

                    Identificador funcObj = c.func;

                    // Valor del argumento: un token se reporta tal cual (las nativas con su temporal);
                    // una expresion se reporta con el temporal que genera.
                    String valorFinal = "";
                    String valorReporte = "";
                    Object[] tokenEvaluado = null;
                    if (!expresionActual.isEmpty()) {
                        if (expresionActual.size() == 1) {
                            tokenEvaluado = expresionActual.get(0);
                            if (tokenEvaluado[1] != null && !tokenEvaluado[1].toString().isEmpty()) {
                                valorFinal = tokenEvaluado[1].toString();
                            } else if (tokenEvaluado[0] != null && tokenEvaluado[0].toString().startsWith("#")) {
                                valorFinal = tokenEvaluado[0].toString();
                            }
                            valorReporte = textoReporte(tokenEvaluado);
                            if (etiqueta(tokenEvaluado).isEmpty() && esIdentificadorLexema(valorFinal) && buscarEnTabla(valorFinal) == null) {
                                contadoresTemporales[VAR]++;
                                valorFinal = PREFIJOS_TEMP[VAR] + contadoresTemporales[VAR];
                                valorReporte = valorFinal;
                            }
                        } else {
                            List<Object[]> prefijaDer = convertirAPrefijo(expresionActual);
                            indiceGlobalPrefijo = 0;
                            int indexInicio = listaCuadruplos.size();
                            generarCuadruplosRecursivo(prefijaDer);

                            if (listaCuadruplos.size() > indexInicio) {
                                String[] ultimoCuad = listaCuadruplos.get(listaCuadruplos.size() - 1);
                                if (ultimoCuad.length > 3 && ultimoCuad[3] != null && ultimoCuad[3].startsWith("T")) {
                                    valorFinal = ultimoCuad[3];
                                }
                            }
                            if (valorFinal.isEmpty() && valorDerechoStr != null && !valorDerechoStr.isEmpty()) valorFinal = valorDerechoStr;
                            valorReporte = valorFinal.isEmpty() ? textoDe(expresionActual) : valorFinal;
                        }
                    }

                    if (funcObj != null) {
                        int totalParamsDeclarados = 0;
                        if (funcObj.numeroPar != null) {
                            totalParamsDeclarados = funcObj.numeroPar;
                        }

                        Identificador paramEsperado = null;
                        for (Identificador id : registroIdentificadores) {
                            if (id.clase != null && id.clase.equals("PAR") &&
                                id.tamañoPar != null && id.tamañoPar.equals(c.nombre) &&
                                id.numeroPar != null && id.numeroPar == contadorArgumentos) {
                                paramEsperado = id;
                                break;
                            }
                        }
                        String topeParam = paramEsperado != null ? paramEsperado.id : "-";

                        // ----------------------------------------------------
                        // REGLA 1150: CANTIDAD DE PARÁMETROS
                        // ----------------------------------------------------
                        if (contadorArgumentos > totalParamsDeclarados) {
                            c.errorRegla15 = true;
                            String errorMsg = "Se enviaron más argumentos (" + contadorArgumentos + ") de los declarados (" + totalParamsDeclarados + ")";
                            c.reglasPend.add(new Object[]{tokenActual[2], "1150", "PARAMETROS DECLARADOS", "Error", valorReporte, ambitoActual, topeParam});
                            c.erroresPend.add(new Object[]{562, errorMsg, c.nombre, "Semantico 2", tokenActual[2]});
                        } else {
                            c.reglasPend.add(new Object[]{tokenActual[2], "1150", "PARAMETROS DECLARADOS", "Acept", valorReporte, ambitoActual, topeParam});
                        }

                        // ----------------------------------------------------
                        // REGLA 1160: TIPO DE PARÁMETROS
                        // ----------------------------------------------------
                        if (!c.errorRegla15 && paramEsperado != null && !expresionActual.isEmpty()) {
                            String tipoEsperado = paramEsperado.tipo;

                            // Inferimos el tipo del argumento
                            String tipoArgumento = "";
                            String et = (tokenEvaluado != null) ? etiqueta(tokenEvaluado) : "";
                            if (et.equals("VOID")) {
                                tipoArgumento = "Void";                         // procedimiento o nativa void
                            } else if (et.equals("ARR") || et.equals("USR")) {
                                Identificador ref = buscarEnTabla(valorFinal);
                                tipoArgumento = (ref != null && ref.tipo != null && !ref.tipo.isEmpty()) ? ref.tipo : "Variant";
                            } else if (valorFinal.startsWith("TBI")) tipoArgumento = "Bin";
                            else if (valorFinal.startsWith("TD")) tipoArgumento = "Dec";
                            else if (valorFinal.startsWith("TO")) tipoArgumento = "Oct";
                            else if (valorFinal.startsWith("TH")) tipoArgumento = "Hex";
                            else if (valorFinal.startsWith("TR")) tipoArgumento = "Real";
                            else if (valorFinal.startsWith("TE")) tipoArgumento = "exp";
                            else if (valorFinal.startsWith("TC")) tipoArgumento = "Cadena";
                            else if (valorFinal.startsWith("TB")) tipoArgumento = "Boolean";
                            else if (valorFinal.startsWith("TV")) tipoArgumento = "Variant";

                            if (tipoArgumento.isEmpty()) {
                                Identificador argVar = buscarEnTabla(valorFinal);
                                if (argVar != null) tipoArgumento = argVar.tipo;
                                else if (tokenEvaluado != null) tipoArgumento = getTipo(tokenEvaluado);
                            }

                            if (tipoEsperado.equals("Decimal")) tipoEsperado = "Dec";
                            if (tipoArgumento.equals("Decimal")) tipoArgumento = "Dec";
                            if (tipoEsperado.equals("Booleana") || tipoEsperado.equals("TBool")) tipoEsperado = "Boolean";
                            if (tipoArgumento.equals("Booleana") || tipoArgumento.equals("TBool")) tipoArgumento = "Boolean";

                            String topeTipo = tipoConT(tipoEsperado);

                            if (!tipoArgumento.equals("Void") && cabeEnTipo(tipoEsperado, tipoArgumento)) {
                                c.reglasPend.add(new Object[]{tokenActual[2], "1160", "PARAMETROS DE TIPO", "Acept", valorReporte, ambitoActual, topeTipo});
                            } else {
                                String errorMsg = "El argumento (" + tipoArgumento + ") no coincide con el parámetro esperado (" + tipoEsperado + ")";
                                c.reglasPend.add(new Object[]{tokenActual[2], "1160", "PARAMETROS DE TIPO", "Error", valorReporte, ambitoActual, topeTipo});
                                c.erroresPend.add(new Object[]{563, errorMsg, paramEsperado.id + " = " + valorReporte, "Semantico 2", tokenActual[2]});
                            }
                        }
                    }
                    c.argsTxt.add(expresionActual.isEmpty() ? "" : textoDe(expresionActual));
                    expresionActual.clear();
                    valorDerechoStr = "";
                }

                pilaSintactica.pop();
                continue;
            }

            // =====================================================================
            // Acción 840: Cierre de la llamada de usuario (faltan argumentos)
            // Restaura la expresion de afuera y le agrega la llamada como UN token.
            // =====================================================================
            if (tope.equals("840")) {
                CtxLlamada c = (!pilaCtx.isEmpty() && pilaCtx.peek() instanceof CtxLlamada) ? (CtxLlamada) pilaCtx.pop() : null;

                if (c != null) {
                    // Primero las reglas 1150/1160 de todos los argumentos de esta llamada
                    listaRegistroReglas.addAll(c.reglasPend);
                    listaErroresSemantico2.addAll(c.erroresPend);

                    int ambitoActual = pilaContadorAmbito.isEmpty() ? 0 : pilaContadorAmbito.peek();
                    Identificador funcObj = c.func;
                    if (funcObj != null && !c.errorRegla15) {
                        int totalParamsDeclarados = funcObj.numeroPar != null ? funcObj.numeroPar : 0;
                        if (c.nArgs < totalParamsDeclarados) {
                            String errorMsg = "Faltan argumentos. Se esperaban " + totalParamsDeclarados + ", pero se enviaron " + c.nArgs;
                            // Tope: el primer parametro que no recibio argumento
                            String paramFaltante = "-";
                            for (Identificador id : registroIdentificadores) {
                                if (id.clase != null && id.clase.equals("PAR") && id.tamañoPar != null && id.tamañoPar.equals(c.nombre)
                                    && id.numeroPar != null && id.numeroPar == c.nArgs + 1) {
                                    paramFaltante = id.id;
                                    break;
                                }
                            }
                            listaRegistroReglas.add(new Object[]{tokenActual[2], "1150", "PARAMETROS DECLARADOS", "Error", "-", ambitoActual, paramFaltante});
                            listaErroresSemantico2.add(new Object[]{564, errorMsg, c.nombre, "Semantico 2", tokenActual[2]});
                        }
                    }

                    String texto = c.nombre + "(" + String.join(", ", c.argsTxt) + ")";
                    expresionActual.clear();
                    expresionActual.addAll(c.exprPrevia);
                    leyendoOperacion = c.leyendoPrev;
                    valorDerechoStr = c.valorDerPrev;
                    if (leyendoOperacion) {
                        // Un procedimiento (sin prefijo de tipo) no regresa valor
                        boolean esProc = funcObj != null && (funcObj.tipo == null || funcObj.tipo.isEmpty())
                                         || (funcObj == null && !c.nombre.isEmpty() && Character.isLetter(c.nombre.charAt(0)));
                        expresionActual.add(new Object[]{c.codigoId, c.nombre, c.linea, texto, esProc ? "VOID" : "USR"});
                        if (expresionActual.size() == 1) valorDerechoStr = c.nombre;
                    }
                } else {
                    leyendoOperacion = false;
                    expresionActual.clear();
                }
                pilaSintactica.pop();
                continue;
            }

            // =====================================================================
            // SEMÁNTICA 3 - Acción 850: Inicio de Función Nativa
            // =====================================================================
            if (tope.equals("850")) {
                Object[] tokFunc = listaTokens.get(i - 1);
                String nombreFunc = tokFunc[1].toString();

                if (leyendoOperacion && !expresionActual.isEmpty()) {
                    Object[] ultimo = expresionActual.get(expresionActual.size() - 1);
                    if (ultimo[1].toString().equals(nombreFunc)) {
                        expresionActual.remove(expresionActual.size() - 1);
                    }
                }

                pilaCtx.push(new ContextoFuncSem3(nombreFunc, leyendoOperacion, valorDerechoStr, expresionActual, tokFunc[2]));

                expresionActual.clear();
                valorDerechoStr = "";
                leyendoOperacion = true;
                pilaSintactica.pop();
                continue;
            }

            // =====================================================================
            // SEMÁNTICA 3 - Acción 851: Inicio de Parámetro de Función
            // =====================================================================
            if (tope.equals("851")) {
                expresionActual.clear();
                valorDerechoStr = "";
                leyendoOperacion = true;
                pilaSintactica.pop();
                continue;
            }

            // =====================================================================
            // SEMÁNTICA 3 - Acción 852: Fin y Validación de Parámetro (Reglas 2002-2010)
            // =====================================================================
            if (tope.equals("852")) {
                ContextoFuncSem3 ctx = (ContextoFuncSem3) pilaCtx.peek();
                ctx.numParam++;
                int numP = ctx.numParam;
                String func = ctx.nombreFunc;
                String fn = func.equalsIgnoreCase("CLEAR") ? "CLEAR" : func.toLowerCase();
                int ambito = pilaContadorAmbito.peek();

                boolean esUnSoloToken = (expresionActual.size() == 1);
                boolean esIdentificador = false;
                boolean esConstante = false;
                String tipoParam = "";
                String lexemaStr = "";
                Object[] tokenEvaluado = null;

                if (esUnSoloToken) {
                    tokenEvaluado = expresionActual.get(0);
                    String lex = tokenEvaluado[1].toString();
                    String et = etiqueta(tokenEvaluado);
                    lexemaStr = textoReporte(tokenEvaluado);

                    int codigoTok = 0;
                    try {
                        codigoTok = Integer.parseInt(tokenEvaluado[0].toString());
                    } catch (NumberFormatException e) {
                        codigoTok = -1;
                    }

                    if (et.equals("VOID")) {
                        // CLEAR, copy, val o un procedimiento: no regresan valor, nunca es aceptado
                        tipoParam = "Void";
                    } else if (et.equals("ARR")) {
                        // Elemento de arreglo: cuenta como id, del tipo del arreglo
                        esIdentificador = true;
                        Identificador ref = buscarEnTabla(lex);
                        tipoParam = (ref != null && ref.tipo != null && !ref.tipo.isEmpty()) ? ref.tipo : "Variant";
                    } else if (et.equals("USR")) {
                        // Llamada a funcion de usuario: temporal del tipo que regresa (no es id)
                        Identificador ref = buscarEnTabla(lex);
                        tipoParam = (ref != null && ref.tipo != null && !ref.tipo.isEmpty()) ? ref.tipo : "Variant";
                    } else if (et.equals("NAT")) {
                        // Temporal de otra funcion nativa
                        tipoParam = getTipo(tokenEvaluado);
                    } else if (lex.equals("null")) {
                        // null no es Variant: no es aceptado por ninguna regla
                        tipoParam = "Null";
                    } else {
                        boolean esTemporal = esTemporalNombre(lex);

                        if (!esTemporal && (codigoTok <= -60 && codigoTok >= -67 || buscarEnTabla(lex) != null)) {
                            esIdentificador = true;
                        }

                        // Si es identificador y NO está en la tabla de símbolos, es Variant puro
                        Identificador idObj = buscarEnTabla(lex);
                        if (esIdentificador && idObj == null) {
                            tipoParam = "Variant";
                            // Igual que en una operacion: un id no declarado consume un temporal Variant
                            contadoresTemporales[VAR]++;
                        } else if (idObj != null) {
                            tipoParam = idObj.tipo;
                        } else {
                            tipoParam = getTipo(tokenEvaluado);
                        }
                        esConstante = !esIdentificador && !esTemporal;
                    }
                }
                else if (expresionActual.size() > 1) {
                    List<Object[]> prefijaParam = convertirAPrefijo(expresionActual);
                    indiceGlobalPrefijo = 0;
                    int indexInicioCuad = listaCuadruplos.size();
                    generarCuadruplosRecursivo(prefijaParam);

                    String valorFinal = "";
                    if (listaCuadruplos.size() > indexInicioCuad) {
                        String[] ultimoCuad = listaCuadruplos.get(listaCuadruplos.size() - 1);
                        if (ultimoCuad.length > 3 && ultimoCuad[3] != null) {
                            valorFinal = ultimoCuad[3];
                        }
                    }

                    int cod = 0;
                    if (valorFinal.startsWith("TBI")) cod = -61;
                    else if (valorFinal.startsWith("TD")) cod = -62;
                    else if (valorFinal.startsWith("TO")) cod = -63;
                    else if (valorFinal.startsWith("TH")) cod = -64;
                    else if (valorFinal.startsWith("TR")) cod = -65;
                    else if (valorFinal.startsWith("TE")) cod = -66;
                    else if (valorFinal.startsWith("TC")) cod = -60;
                    else if (valorFinal.startsWith("TB")) cod = -67;
                    else if (valorFinal.startsWith("TV")) cod = 0;

                    tokenEvaluado = new Object[]{cod, valorFinal, tokenActual[2]};
                    tipoParam = getTipo(tokenEvaluado);
                    lexemaStr = valorFinal.isEmpty() ? textoDe(expresionActual) : valorFinal;
                }

                // Normalización local
                String tipoNorm = tipoParam != null ? tipoParam.trim() : "";
                if (numP == 1) {
                    ctx.tipoParam1 = tipoNorm;
                }

                String reglaCve = "";
                String topeRegla = "";
                String descRegla = fn + "_p" + numP;
                boolean valido = false;

                boolean esEnteroBase = tipoNorm.equals("Bin") || tipoNorm.equals("Dec") || tipoNorm.equals("Oct") || tipoNorm.equals("Hex");
                boolean esRealOExp = tipoNorm.equals("Real") || tipoNorm.equals("exp");
                boolean esDecRealExp = tipoNorm.equals("Dec") || esRealOExp;
                boolean esNumerico = esEnteroBase || esRealOExp;

                // Variant (id no declarado) es comodín
                boolean esVariant = tipoNorm.equals("Variant") || tipoNorm.isEmpty();

                if ((func.equalsIgnoreCase("SQRT") || func.equalsIgnoreCase("SQRTV") || func.equalsIgnoreCase("sin") ||
                     func.equalsIgnoreCase("cos") || func.equalsIgnoreCase("tan") || func.equalsIgnoreCase("sqr")) && numP == 1) {
                    reglaCve = "2002"; topeRegla = "TDec/TReal/Texp/id";
                    valido = esDecRealExp || esVariant;
                }
                else if (func.equalsIgnoreCase("POW") && numP == 1) {
                    reglaCve = "2003"; topeRegla = "TBin/TDec/TOct/THex/TReal/Texp/id";
                    valido = esNumerico || esVariant;
                }
                else if ((func.equalsIgnoreCase("POW") && numP == 2) || (func.equalsIgnoreCase("SQRTV") && numP == 2)) {
                    reglaCve = "2004"; topeRegla = "TDec/id";
                    valido = tipoNorm.equals("Dec") || esVariant;
                }
                else if (func.equalsIgnoreCase("val") && numP == 3) {
                    reglaCve = "2004"; topeRegla = "TDec/id (2,8,10,16)";
                    // Dec (id o temporal); si es constante solo 2, 8, 10 o 16
                    boolean baseValida = !esConstante || (tokenEvaluado != null &&
                            Arrays.asList("2", "8", "10", "16").contains(String.valueOf(tokenEvaluado[1]).trim()));
                    valido = (tipoNorm.equals("Dec") && baseValida) || esVariant;
                }
                else if ((func.equalsIgnoreCase("STRLEN") && numP == 1) ||
                         (func.equalsIgnoreCase("concat") && (numP == 1 || numP == 2)) ||
                         (func.equalsIgnoreCase("copy") && numP == 2)) {
                    reglaCve = "2005"; topeRegla = "TCadena/id";
                    valido = tipoNorm.equals("Cadena") || esVariant;
                }
                else if ((func.equalsIgnoreCase("copy") && numP == 1) ||
                         (func.equalsIgnoreCase("str") && numP == 2)) {
                    reglaCve = "2006"; topeRegla = "id TCadena";
                    valido = (esIdentificador && tipoNorm.equals("Cadena")) || esVariant;
                }
                else if (func.equalsIgnoreCase("val") && (numP == 1 || numP == 2)) {
                    reglaCve = "2007"; topeRegla = "id TBin/TDec/TOct/THex";
                    valido = (esIdentificador && esEnteroBase) || esVariant;
                }
                else if (func.equalsIgnoreCase("str") && numP == 1) {
                    reglaCve = "2008"; topeRegla = "TBin/TDec/TOct/THex/TReal/Texp/TBoolean/id";
                    valido = esNumerico || tipoNorm.equals("Boolean") || esVariant;
                }
                else if ((func.equalsIgnoreCase("chr") || func.equalsIgnoreCase("inc") || func.equalsIgnoreCase("dec")) && numP == 1) {
                    reglaCve = "2009"; topeRegla = "TBin/TDec/TOct/THex/id";
                    valido = esEnteroBase || esVariant;
                }
                else if ((func.equalsIgnoreCase("pred") || func.equalsIgnoreCase("succ")) && numP == 1) {
                    reglaCve = "2010"; topeRegla = "TReal/Texp/id";
                    valido = esRealOExp || esVariant;
                }

                if (numP == 1) ctx.p1Valido = valido;
                ctx.textosParams.add(expresionActual.isEmpty() ? "" : textoDe(expresionActual));

                String edo = valido ? "Acept" : "ERROR";
                listaRegistroReglas.add(new Object[]{tokenActual[2], reglaCve, descRegla, edo, lexemaStr, ambito, topeRegla});

                if (!valido) {
                    ctx.todosParamsValidos = false;
                    String msgError = "El parámetro '" + lexemaStr + "' (Tipo: " + tipoNorm + ") no es válido para " + func;
                    listaErroresSemantico3.add(new Object[]{Integer.parseInt(reglaCve), msgError, lexemaStr, "Semantico 3", tokenActual[2]});
                }

                expresionActual.clear();
                valorDerechoStr = "";
                pilaSintactica.pop();
                continue;
            }

            // =====================================================================
            // SEMÁNTICA 3 - Acción 853: Cierre de Función, Salida y Regreso
            // La salida depende solo del primer parametro y siempre se registra "Acept".
            // =====================================================================
            if (tope.equals("853")) {
                ContextoFuncSem3 ctx = (ContextoFuncSem3) pilaCtx.pop();
                String func = ctx.nombreFunc;
                String fn = func.equalsIgnoreCase("CLEAR") ? "CLEAR" : func.toLowerCase();
                int ambito = pilaContadorAmbito.peek();

                if (func.equalsIgnoreCase("CLEAR")) {
                    listaRegistroReglas.add(new Object[]{ctx.lineaToken, "2001", "CLEAR", "Acept", "-", ambito, "Sin parámetros"});
                }

                String reglaSalida = "";
                String tipoSalida = "";
                int constanteIdx = -1;
                int codigoTemporal = 0;
                boolean p1Entero = ctx.tipoParam1.equals("Bin") || ctx.tipoParam1.equals("Dec") ||
                                   ctx.tipoParam1.equals("Oct") || ctx.tipoParam1.equals("Hex");

                if (func.equalsIgnoreCase("CLEAR") || func.equalsIgnoreCase("copy") || func.equalsIgnoreCase("val")) {
                    reglaSalida = "2011";
                    tipoSalida = "void";
                }
                else if (func.equalsIgnoreCase("SQRT") || func.equalsIgnoreCase("SQRTV") || func.equalsIgnoreCase("sin") ||
                         func.equalsIgnoreCase("cos") || func.equalsIgnoreCase("tan") || func.equalsIgnoreCase("sqr")) {
                    reglaSalida = "2012";
                    tipoSalida = "exp";
                    constanteIdx = EXP; codigoTemporal = -66;
                }
                else if (func.equalsIgnoreCase("POW")) {
                    reglaSalida = "2013";
                    // Entero valido en p1 -> ese tipo; cualquier otro caso -> exp
                    tipoSalida = (ctx.p1Valido && p1Entero) ? ctx.tipoParam1 : "exp";
                    switch (tipoSalida) {
                        case "Bin": constanteIdx = BIN; codigoTemporal = -61; break;
                        case "Dec": constanteIdx = DEC; codigoTemporal = -62; break;
                        case "Oct": constanteIdx = OCT; codigoTemporal = -63; break;
                        case "Hex": constanteIdx = HEX; codigoTemporal = -64; break;
                        default: constanteIdx = EXP; codigoTemporal = -66; break;
                    }
                }
                else if (func.equalsIgnoreCase("STRLEN")) {
                    reglaSalida = "2014";
                    tipoSalida = "Dec";
                    constanteIdx = DEC; codigoTemporal = -62;
                }
                else if (func.equalsIgnoreCase("concat") || func.equalsIgnoreCase("chr")) {
                    reglaSalida = "2015";
                    tipoSalida = "Cadena";
                    constanteIdx = CAD; codigoTemporal = -60;
                }
                else if (func.equalsIgnoreCase("str")) {
                    reglaSalida = "2016";
                    tipoSalida = "Boolean";
                    constanteIdx = BOOL; codigoTemporal = -67;
                }
                else if (func.equalsIgnoreCase("pred") || func.equalsIgnoreCase("succ")) {
                    reglaSalida = "2017";
                    // Real valido en p1 -> Real; cualquier otro caso -> exp
                    tipoSalida = (ctx.p1Valido && ctx.tipoParam1.equals("Real")) ? "Real" : "exp";
                    if (tipoSalida.equals("Real")) { constanteIdx = REAL; codigoTemporal = -65; }
                    else { constanteIdx = EXP; codigoTemporal = -66; }
                }
                else if (func.equalsIgnoreCase("inc") || func.equalsIgnoreCase("dec")) {
                    reglaSalida = "2018";
                    // Entero valido en p1 -> ese tipo; cualquier otro caso -> Dec
                    tipoSalida = (ctx.p1Valido && p1Entero) ? ctx.tipoParam1 : "Dec";
                    switch (tipoSalida) {
                        case "Bin": constanteIdx = BIN; codigoTemporal = -61; break;
                        case "Oct": constanteIdx = OCT; codigoTemporal = -63; break;
                        case "Hex": constanteIdx = HEX; codigoTemporal = -64; break;
                        default: constanteIdx = DEC; codigoTemporal = -62; break;
                    }
                }

                // Texto original de la llamada, ej. concat(@SA, @SArr[1])
                String textoLlamada = func.equalsIgnoreCase("CLEAR") ? func : func + "(" + String.join(", ", ctx.textosParams) + ")";

                // Restauramos la grabadora a como estaba antes de la función
                expresionActual.clear();
                expresionActual.addAll(ctx.exprPrevia);
                leyendoOperacion = ctx.leyendoOpPrevio;
                valorDerechoStr = ctx.valorDerPrevio;

                if (!tipoSalida.equals("void") && constanteIdx != -1) {
                    contadoresTemporales[constanteIdx]++;
                    String nombreTemp = PREFIJOS_TEMP[constanteIdx] + contadoresTemporales[constanteIdx];
                    Object[] tokenTemp = new Object[]{codigoTemporal, nombreTemp, ctx.lineaToken, textoLlamada, "NAT"};

                    listaRegistroReglas.add(new Object[]{ctx.lineaToken, reglaSalida, fn, "Acept", nombreTemp, ambito, tipoConT(tipoSalida)});

                    if (leyendoOperacion) {
                        expresionActual.add(tokenTemp);
                        if (expresionActual.size() == 1) {
                            valorDerechoStr = nombreTemp;
                        }
                    }
                } else {
                    listaRegistroReglas.add(new Object[]{ctx.lineaToken, reglaSalida, fn, "Acept", "TVoid", ambito, "TVoid"});
                    if (leyendoOperacion) {
                        // Void usado como parametro/operando: se marca para que nunca sea aceptado
                        expresionActual.add(new Object[]{0, textoLlamada, ctx.lineaToken, textoLlamada, "VOID"});
                        if (expresionActual.size() == 1) valorDerechoStr = textoLlamada;
                    }
                }

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

                if (!vieneDeArreglo) {
                    idIzquierdoStr = ultimoIdVisto;
                }
                vieneDeArreglo = false; // Apagamos la bandera
                leyendoOperacion = true;
                expresionActual.clear();
                lineaOperacionActual = (int) tokenActual[2]; // Asumiendo que [2] es la línea
                if (parteForClasico == 1) {
                    huboAsignacionFor = true;
                    // Guardamos la primera mitad (ej. "#Dx =")
                    lexemaAsignacionFor = idIzquierdoStr + " =";
                }
                // En el inicio del for, lo que va despues del '=' es uso, no declaracion
                // (antes for (#Dk = inc(#Dx); ...) declaraba #Dx dentro del for)
                boolean quitarDeclaracionFor = parteForClasico == 1 && claseActual.equals("VAR");

                // ==========================================================
                // 2. NUEVO: REGLA 9 - Validar clase del identificador (CVE 1090)
                // ==========================================================
                if (!EsDeclaracion) {
                    Identificador idObj = buscarEnTabla(idIzquierdoStr);
                    String cveRegla9 = "1090";
                    String descRegla9 = "ID ES COMPATIBLE";
                    int ambitoActual = pilaContadorAmbito.isEmpty() ? 0 : pilaContadorAmbito.peek();

                    // 1. Asumimos "VAR" por defecto. Solo cambiamos si el ID existe y tiene una clase definida.
                    String claseId = "VAR";
                    if (idObj != null && idObj.clase != null && !idObj.clase.isEmpty()) {
                        claseId = idObj.clase.toUpperCase();
                    }

                    // 2. Evaluamos la clase obtenida (o la asumida)
                    if (claseId.equals("FUNC") || claseId.equals("CONST")) {
                        // FALLA REGLA 9
                        String errorMsg = "No se puede asignar a clase " + claseId;
                        //System.out.println("error " + errorMsg + " -> " + idIzquierdoStr);
                        
                        listaRegistroReglas.add(new Object[]{
                            tokenActual[2], cveRegla9, descRegla9, 
                            "Error", idIzquierdoStr, ambitoActual,"id"
                        });
                        listaErroresSemantico2.add(new Object[]{
                            558, errorMsg, idIzquierdoStr, "Semantico 2", tokenActual[2]
                        });
                        
                    } else {
                        // CUMPLE REGLA 9
                        //System.out.println("bien = Clase válida (" + claseId + ") para asignación: " + idIzquierdoStr);
                        
                        listaRegistroReglas.add(new Object[]{
                            tokenActual[2], cveRegla9, descRegla9, 
                            "Acept", idIzquierdoStr, ambitoActual,"id"
                        });
                    }
                }
                // ==========================================================
                if (quitarDeclaracionFor) claseActual = "";

                pilaSintactica.pop();
                continue;
            }
            //CHECAR QUE NO VAYA POR AQUI CUANDO ESTAMOS EN FOR
            if (tope.equals("812")) {
                leyendoOperacion = false;
                // Convertimos a prefijo el lado derecho
                List<Object[]> prefijaDer = convertirAPrefijo(expresionActual);
                
                // Creamos las listas completas (Izquierda + Derecha)
                List<Object[]> infijaCompleta = new ArrayList<>();
                List<Object[]> prefijaCompleta = new ArrayList<>();
                idIzquierdoStr = (tokenIzquierdo != null) ? (String) tokenIzquierdo[1] : "";
                //System.out.println(idIzquierdoStr + "  tok asig "+tokenAsignacion) ;
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
                // System.out.println("expresion actual " + prefijaCompleta);

                int indexInicioCuadruplos = listaCuadruplos.size(); // Contamos cuántos hay antes
                // 1. Guardamos el estado exacto de los contadores ANTES de la línea
                int[] contadoresAntes = contadoresTemporales.clone();
                ladoDerechoVoid = expresionActual.size() == 1 && etiqueta(expresionActual.get(0)).equals("VOID");
                generarCuadruplosRecursivo(prefijaCompleta);
                ladoDerechoVoid = false;
                ladoIzquierdoArreglo = false;

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
                    asignacionFinal = idIzquierdoStr + " = " + valorDerechoStr;
                } else if (!ultimoTemporal.isEmpty()) {
                    asignacionFinal = "Eval -> " + ultimoTemporal;
                }

                // Guardamos: [Linea, ArregloContadores, AsignacionFinal]
                if (!ignorarParaExcel){
                    // 3. Calculamos la diferencia: cuántos se hicieron SOLAMENTE en esta línea
                    int[] contadoresSoloEstaLinea = new int[9];
                    for (int j = 0; j < 9; j++) {
                        contadoresSoloEstaLinea[j] = contadoresTemporales[j] - contadoresAntes[j];
                    }
                    listaResumenOperaciones.add(new Object[]{lineaOperacionActual, contadoresSoloEstaLinea, asignacionFinal});
                }
                idIzquierdoStr = "";
                ultimoTemporal = "";
                valorDerechoStr = ""; // <-- ¡Aquí se reinicia!
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
                    // Los indices de arreglo ya no usan un buffer aparte: cada arreglo
                    // abre su contexto en pilaCtx y graba en expresionActual.
                    expresionActual.add(new Object[]{tokenActual[0], tokenActual[1], tokenActual[2]});
                    if (expresionActual.size() == 1) {
                        valorDerechoStr = (String) tokenActual[1];
                    }
                }
                if (parteForClasico == 3) {
                        // Capturar ID izquierdo si aún no hay operador
                        if (terminalActual.equals("id") && !vieneDeArreglo && tokenAsignacion == null) {
                            tokenIzquierdo = tokenActual;
                        } 
                        // Si llega un operador de asignación, lo capturamos
                        else if (terminalActual.equals("=") || terminalActual.equals("+=") || 
                            terminalActual.equals("-=") || terminalActual.equals("*=") || 
                            terminalActual.equals("/=") || terminalActual.equals("%=") || 
                            terminalActual.equals("&=") || terminalActual.equals("^=") || 
                            terminalActual.equals("<<=") || terminalActual.equals(">>=") || 
                            terminalActual.equals(">>>=") || terminalActual.equals("=>")) {
                            
                            tokenAsignacion = tokenActual;
                            huboAsignacionFor = true;
                            lexemaAsignacionFor = ultimoIdVisto + " " + tokenActual[1];
                            
                            // LIMPIEZA CLAVE: Borramos el ID y el '+=' de la grabadora
                            // para que a partir de aquí solo guarde el lado derecho (ej. "b")
                            expresionActual.clear();
                            valorDerechoStr = "";
                        }
                    }
                 else {
                    //System.out.println("else de " + tokenActual[1] + " es "+tope);
                    if (terminalActual.equals("id")) {
                        // Solo el id ANTES del '=' es el lado izquierdo (los ids de la derecha ya no lo pisan)
                        if (!vieneDeArreglo && tokenAsignacion == null){
                            tokenIzquierdo = tokenActual;
                        }
                    } else if (terminalActual.equals("=") || terminalActual.equals("+=") || 
                        terminalActual.equals("-=") || terminalActual.equals("*=") || 
                        terminalActual.equals("/=") || terminalActual.equals("%=") || 
                        terminalActual.equals("&=") || terminalActual.equals("^=") || 
                        terminalActual.equals("<<=") || terminalActual.equals(">>=") || 
                        terminalActual.equals(">>>=") || terminalActual.equals("=>")) {
                    
                    tokenAsignacion = tokenActual;
                }
                }

                if (terminalActual.equals("id")){
                    String lexemaActual = (String) tokenActual[1];
                    int ambitoNum = pilaContadorAmbito.peek();
                    ultimoIdVisto = lexemaActual;

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
                            ultimoIdDuplicado = true;

                            // Una def duplicada se registra en la tabla (con sus parametros) pero no
                            // reemplaza a la original en el ambito: las llamadas siguen usando la primera.
                            if (claseActual.equals("FUNC")) {
                                Identificador dupId = new Identificador(lexemaActual, getTipo(tokenActual), claseActual, ambitoNum);
                                dupId.numeroPar = 0;
                                dupId.tamañoPar = String.valueOf(numAmbito);
                                funcionActual = dupId;
                                registroIdentificadores.add(dupId);
                            }
                        }
                        else{
                            ultimoIdDuplicado = false;
                            boolean tipoValido = true;
        
                            // ==============================================================
                            // NUEVO: Comprobar que el primer ID (tipo de registro) exista
                            // ==============================================================
                            if (!tipoPersonalizado.isEmpty()) {
                                Identificador definicionRegistro = buscarEnTabla(tipoPersonalizado);
                                
                                if (definicionRegistro == null) {
                                    tipoValido = false; // Bloqueamos el guardado de la variable
                                    
                                    String descripcion = "El tipo de registro '" + tipoPersonalizado + "' no ha sido declarado previamente."; 
                                    Object[] datosError = {545, descripcion, tokenActual[1], "Ambito", tokenActual[2], ambitoNum};
                                    listaErroresSemantico.add(datosError);
                                    arregloActual = null;
                                }
                            }
                            // Solo guardamos el ID si el tipo es válido (o si es una variable primitiva normal)
                            if (tipoValido) {
                                String tipo = tipoPersonalizado.isEmpty() ? getTipo(tokenActual) : tipoPersonalizado;
                                Identificador nuevoId = new Identificador(lexemaActual, tipo, claseActual, ambitoNum);

                                if (claseActual.equals("FUNC") || (claseActual.equals("REG") && esDefinicionRegistro)) {
                                    funcionActual = nuevoId;
                                    nuevoId.numeroPar = 0;
                                    nuevoId.tamañoPar = String.valueOf(numAmbito);
                                }
                                if (claseActual.equals("PAR")) {
                                    if (funcionActual != null) {
                                        funcionActual.numeroPar++;
                                        nuevoId.numeroPar = funcionActual.numeroPar;
                                        nuevoId.tamañoPar = funcionActual.id;
                                    }
                                }
                                if (claseActual.equals("VAR") || (claseActual.equals("REG") && !esDefinicionRegistro)) {
                                    arregloActual = nuevoId;
                                }
                                
                                ambitoLocal.put(lexemaActual, nuevoId);
                                registroIdentificadores.add(nuevoId);
                            }
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
                        // =================================================================
                        // REGLA 10: Un ID debe ser declarado antes de ser utilizado
                        // =================================================================
                        String cveRegla10 = "1100"; // Asegúrate de poner el CVE correcto
                        String descRegla10 = "ID DECLARADO";
                        
                        if (!existe) {
                            // FALLA REGLA 10
                            String descripcion = "Error Semántico: '" + lexemaActual + "' no ha sido declarada. " + ambitoNum;
                            Object[] datosError = {543, descripcion, tokenActual[1], "Ambito", tokenActual[2],ambitoNum};
                            listaErroresSemantico.add(datosError);
                            
                            listaRegistroReglas.add(new Object[]{
                                tokenActual[2], cveRegla10, descRegla10, 
                                "Error", lexemaActual, ambitoNum,"id"
                            });
                        } else {
                            // CUMPLE REGLA 10
                            listaRegistroReglas.add(new Object[]{
                                tokenActual[2], cveRegla10, descRegla10, 
                                "Acept", lexemaActual, ambitoNum,"id"
                            });
                        }
                        // =================================================================
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
            case "|": 
                return 1;
            case "||": 
                return 2;
            case "&&": 
            case "&": 
                return 3;   // Ambos AND tienen la misma prioridad aquí
            case "<": 
            case ">": 
            case "<=": 
            case ">=": 
            case "==": 
            case "!=": 
                return 4;   // Todos los relacionales y de igualdad juntos
            case "<<": 
            case ">>": 
            case ">>>": 
                return 5;
            case "+": 
            case "-": 
                return 6;
            case "#": 
                return 7;   // El XOR a nivel de bits va antes de la suma
            case "*": 
            case "/": 
            case "%": 
                return 8;
            case "^": 
                return 9;   // Potencia sigue siendo el rey
            default: 
                return -1;
        }
    }
    private boolean esOperadorBinario(String op) {
        return op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%") || op.equals("^") ||
               op.equals("<") || op.equals("<=") || op.equals(">") || op.equals(">=") || op.equals("==") || op.equals("!=") ||
               op.equals("&&") || op.equals("||") || op.equals("&") || op.equals("|") ||
               op.equals("<<") || op.equals(">>") || op.equals(">>>") || op.equals("#");
    }

    private boolean esAsignacion(String op) {
    return op.equals("=") || op.equals("+=") || op.equals("-=") || 
           op.equals("*=") || op.equals("/=") || op.equals("%=") || 
           op.equals("&=") || op.equals("^=") || op.equals("<<=") || 
           op.equals(">>=") || op.equals(">>>=") || op.equals("=>");
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
        // =========================================================
        // NUEVO: IMPRIMIR EL PREFIJO EN CONSOLA
        // =========================================================
        if (!prefijaInvertida.isEmpty()) {
            StringBuilder cadenaPrefijo = new StringBuilder();
            int lineaOperacion = -1;

            for (Object[] token : prefijaInvertida) {
                // Concatenamos el lexema con un espacio
                cadenaPrefijo.append((String) token[1]).append(" ");
                
                // Extraemos la línea del primer token disponible
                if (lineaOperacion == -1 && token.length > 2) {
                    // Asumimos que el índice 2 guarda la línea entera, según tus códigos anteriores
                    lineaOperacion = (int) token[2]; 
                }
            }
            
            // Imprimimos imitando el formato del archivo de prueba
            // System.out.println("Línea -> " + lineaOperacion + " PREFIJO: " + cadenaPrefijo.toString().trim());
        }
        // =========================================================
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
        // System.out.println("Cuádruplos generados en memoria exitosamente. Total: " + listaCuadruplos.size());
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
            if (val1.equals("TVoid") || val2.equals("TVoid")) {
                // Operar con un valor void es error de Semantica 1; el resultado queda Variant
                listaErroresSemantico1.add(new Object[]{544, "Incompatibilidad de tipos: no se puede operar con un valor void",
                                                        val1 + " " + lexemaActual + " " + val2, "Semantico1", tokenActual[2]});
                tipoResultado = VAR;
            }

            // ==========================================
            // ¡REGLA APLICADA! Incompatibilidad -> Variant
            // ==========================================
            if (tipoResultado == E) {
                String descripcion = "Incompatibilidad de tipos: no se puede aplicar '" + lexemaActual + 
                                "' entre " + NOMBRES[tipo1] + " y " + NOMBRES[tipo2];
                Object[] datosError = {544, descripcion, val1 + " " + lexemaActual +" "+ val2, "Semantico1", tokenActual[2]};
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
            
            //System.out.println(lexemaActual + "," + val1 + "," + val2 + "," + temporalAsignado);
            listaCuadruplos.add(new String[]{lexemaActual, nodoIzq[0], nodoDer[0], temporalAsignado});
            return new String[] {temporalAsignado, String.valueOf(tipoResultado)};
        } 
        // CASO 2: Asignación
        else if (esAsignacion(lexemaActual)) {
            int varAntesIzq = contadoresTemporales[VAR];
            String[] nodoIzq = generarCuadruplosRecursivo(prefijo);
            // Un arreglo no declarado a la izquierda (ej. #Dnoexiste[1] = 1) no consume temporal Variant
            if (ladoIzquierdoArreglo && contadoresTemporales[VAR] == varAntesIzq + 1 && nodoIzq[0].startsWith("TV")) {
                contadoresTemporales[VAR]--;
            }
            String[] nodoDer = generarCuadruplosRecursivo(prefijo);
            
            int tipoDestino = Integer.parseInt(nodoIzq[1]);
            int tipoValor = Integer.parseInt(nodoDer[1]);
            String val1 = nodoIzq[0];
            String val2 = nodoDer[0];
            //System.out.println(val1 + " y val2 " + val2);
            boolean esCompatible = false;

            // A) Asignación Directa (=)
            if (lexemaActual.equals("=") || lexemaActual.equals("=>")) {
                // Son compatibles si son el mismo tipo, o si alguno es comodín VAR
                
                if (cabeEnTipo(tipoDestino, tipoValor)) {
                    esCompatible = true;
                }
                // 4) Un valor void (CLEAR, procedimiento) nunca se puede asignar
                if (ladoDerechoVoid) esCompatible = false;
            } 
            // B) Asignaciones Compuestas (+=, *=, <<=, etc.)
            else {
                // 1. Verificamos que la operación subyacente sea válida usando tus matrices
                int tipoResultadoOp = evaluarOperacion(tipoDestino, tipoValor, lexemaActual);
                
                // 2. Si la operación no dio Error (E), verificamos que el resultado quepa en el destino
                // El resultado de la operacion debe caber en el destino (ej. #Dx /= 2 da Real: no cabe en Dec)
                if (tipoResultadoOp != E && tipoResultadoOp != VAR && cabeEnTipo(tipoDestino, tipoResultadoOp)) {
                    esCompatible = true;
                }
            }
            // ==========================================================
            // PREPARACIÓN DE DATOS PARA REGLA 2
            // ==========================================================
            String cveRegla2 = "";
            // Verificamos qué CVE corresponde según el operador
            if (lexemaActual.equals("=") || lexemaActual.equals("=>")) {
                cveRegla2 = "1020"; // Asignación básica[cite: 5]
            } else if (lexemaActual.equals("+=")) {
                cveRegla2 = "1021"; // Asignación +=[cite: 5]
            } else {
                cveRegla2 = "1022"; // Asignación /=, *=, -=, %=, etc.[cite: 5]
            }

            String descRegla2 = "LADO IZQ. IGUAL A DER.";
            String lexemaCompleto = val1 + " " + lexemaActual + " " + val2; // Ej. "#Dacum += 5"
            int ambitoActual =  pilaContadorAmbito.peek();

            // Si falló la validación, lanzamos el error 547
            if (!esCompatible) {
                String descripcion = "Incompatibilidad de tipos en asignación: no se puede aplicar '" + lexemaActual + 
                                    "' entre " + NOMBRES[tipoDestino] + " y " + NOMBRES[tipoValor];
                Object[] datosError = {544, descripcion, val1 + " " + lexemaActual +" "+ val2, "Semantico1", tokenActual[2]};
                listaErroresSemantico1.add(datosError);
                // REGISTRO REGLA 2 (FALLA)
                //System.out.println("error " + descripcion);
                if (!ignorarParaExcel) listaRegistroReglas.add(new Object[]{
                    tokenActual[2], 
                    cveRegla2, 
                    descRegla2, 
                    "Error", 
                    lexemaCompleto, 
                    ambitoActual,
                    "id"
                });
            }
            else {
                // REGISTRO REGLA 2 (CUMPLE)
                //System.out.println("bien = Asignación compatible: " + lexemaCompleto);
                if (!ignorarParaExcel) listaRegistroReglas.add(new Object[]{
                    tokenActual[2], 
                    cveRegla2, 
                    descRegla2, 
                    "Acept", 
                    lexemaCompleto, 
                    ambitoActual,
                    "id"
                });
            }
            
            // Aquí generas el cuádruplo de la asignación (usualmente se deja temporal vacío en el cuádruplo para asignaciones)
            listaCuadruplos.add(new String[]{lexemaActual, nodoIzq[0], nodoDer[0], "-"});
            
            return nodoIzq; // En asignaciones, el tipo resultante suele ser el del lado izquierdo
        }
        // CASO 3: Operando (Variable o Número)
        else {
            // Un valor void (CLEAR, copy, procedimiento) no consume temporal
            if (etiqueta(tokenActual).equals("VOID")) {
                return new String[] {"TVoid", String.valueOf(VAR)};
            }
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
        // El temporal de una funcion nativa (ej. TD7) trae el codigo de su tipo:
        // no es un id, no se busca en la tabla (si no, se volvia Variant)
        if (esTemporalNombre(String.valueOf(token[1])) && !getTipo(token).isEmpty()) {
            return convertirStringAInt(getTipo(token));
        }
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

        // POT y operaciones de bits
        if (operador.equals("%") || operador.equals("%=") || 
            operador.equals(">>>") || operador.equals(">>>=") || 
            operador.equals(">>") || operador.equals(">>=") || 
            operador.equals("<<") || operador.equals("<<=") || 
            operador.equals("^") || operador.equals("^=")) {
            return POT[tipo1][tipo2];
        }
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
        if (operador.equals("&&") || operador.equals("||") || operador.equals("&") || operador.equals("&=") || operador.equals("|") || operador.equals("#")) {
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
        fc.setSelectedFile(new File("Semantica3-BrandonFornesRubio.xlsx"));
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
            if (listaErroresSemantico2 != null) {
                erroresUnificados.addAll(listaErroresSemantico2);
            }
            if (listaErroresSemantico3 != null) {
                erroresUnificados.addAll(listaErroresSemantico3);
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
            // 8. NUEVA HOJA: SEMANTICA 2 - BASE (Actualizada con columna "Función")
            // =========================================================================
            Sheet hojaSemanticaBase = workbook.createSheet("Semantica 2-Base");
            String[] cabeceraBase = {"Regla", "Función", "Tope Pila", "Valor Real", "Linea", "Edo", "Ambito"};
            crearFilaCabecera(hojaSemanticaBase, cabeceraBase, estiloCabecera);

            int filaBaseIndex = 1;
            
            // Mapas para calcular "Semantica 2-Total" (Solo reglas < 2000)
            Map<String, Integer> conteoAparecen = new HashMap<>();
            Map<String, Integer> conteoAceptadas = new HashMap<>();
            Map<String, Integer> conteoErrores = new HashMap<>();

            // Mapas para calcular "Semantica 3" (Solo reglas >= 2000)
            Map<String, Integer> sem3Entradas = new TreeMap<>(); // Entradas (Aparición de func_pX)
            Map<String, Integer> sem3Salidas = new TreeMap<>();  // Salida (Aparición de func)
            Map<String, Integer> sem3Acept = new HashMap<>();    // Aceptados de cada uno
            Map<String, Integer> sem3Error = new HashMap<>();    // Errores de cada uno
            Set<String> todasLasEntidadesSem3 = new TreeSet<>(); // Orden natural: CLEAR (mayusculas) queda primero

            if (listaRegistroReglas != null) {
                for (Object[] regla : listaRegistroReglas) {
                    Row fila = hojaSemanticaBase.createRow(filaBaseIndex++);
                    
                    String reglaCVE = regla.length > 1 && regla[1] != null ? regla[1].toString().trim() : "-";
                    String funcionCol = "-"; // Por defecto vacío para reglas Sem2
                    String topePila = regla.length > 6 && regla[6] != null ? regla[6].toString() : "-";
                    String valorReal = regla.length > 4 && regla[4] != null ? regla[4].toString() : "-";
                    String linea = regla.length > 0 && regla[0] != null ? regla[0].toString() : "-";
                    String estado = regla.length > 3 && regla[3] != null ? regla[3].toString() : "-";
                    String ambito = regla.length > 5 && regla[5] != null ? regla[5].toString() : "-";

                    int numRegla = -1;
                    try { numRegla = Integer.parseInt(reglaCVE); } catch (Exception ignored) {}
                    
                    // Asegurar que si no hay tope válido ponga "-"
                    if (topePila.trim().isEmpty() || topePila.equalsIgnoreCase("null")) topePila = "-";
                    // Formato del reporte: estado de error en mayusculas y temporales con nombre largo (TD3 -> TDec3)
                    if (estado.equalsIgnoreCase("error")) estado = "ERROR";
                    valorReal = temporalParaReporte(valorReal);

                    // Escribir columnas en la Hoja Base (Regla, Linea y Ambito como numero)
                    escribirNumeroOTexto(fila.createCell(0), reglaCVE);
                    fila.createCell(2).setCellValue(topePila);
                    fila.createCell(3).setCellValue(valorReal);
                    escribirNumeroOTexto(fila.createCell(4), linea);
                    fila.createCell(5).setCellValue(estado);
                    escribirNumeroOTexto(fila.createCell(6), ambito);

                    // ========================================================
                    // PROCESAMIENTO DEPENDIENDO DE LA FASE (SEM 2 vs SEM 3)
                    // ========================================================
                    if (!reglaCVE.equals("-")) {
                        
                        if (numRegla < 2000) {
                            // ES SEMÁNTICA 2: Cargar columna Función con "-" (ya lo hizo la celda 1)
                            fila.createCell(1).setCellValue(funcionCol);
                            
                            // Acumular contadores para "Semantica 2-Total"
                            conteoAparecen.put(reglaCVE, conteoAparecen.getOrDefault(reglaCVE, 0) + 1);
                            if (estado.toLowerCase().contains("acept") || estado.toLowerCase().contains("bien")) {
                                conteoAceptadas.put(reglaCVE, conteoAceptadas.getOrDefault(reglaCVE, 0) + 1);
                            } else if (estado.toLowerCase().contains("error")) {
                                conteoErrores.put(reglaCVE, conteoErrores.getOrDefault(reglaCVE, 0) + 1);
                            }
                        } 
                        else if (numRegla >= 2001 && numRegla <= 2018) {
                            // ES SEMÁNTICA 3
                            funcionCol = regla.length > 2 && regla[2] != null ? regla[2].toString().trim() : "-";
                            fila.createCell(1).setCellValue(funcionCol); 

                            if (!funcionCol.equals("-")) {
                                todasLasEntidadesSem3.add(funcionCol);
                                
                                // ¿Es parámetro de entrada o salida de función?
                                if (funcionCol.matches(".*_p\\d+")) {
                                    sem3Entradas.put(funcionCol, sem3Entradas.getOrDefault(funcionCol, 0) + 1);
                                    
                                    // Aceptados y Errores de parámetros
                                    if (estado.toLowerCase().contains("acept") || estado.toLowerCase().contains("bien")) {
                                        sem3Acept.put(funcionCol, sem3Acept.getOrDefault(funcionCol, 0) + 1);
                                    } else if (estado.toLowerCase().contains("error")) {
                                        sem3Error.put(funcionCol, sem3Error.getOrDefault(funcionCol, 0) + 1);
                                    }
                                } 
                                // SOLUCIÓN: Solo contamos como "Salida" si la regla es >= 2011
                                else if (numRegla >= 2011) {
                                    sem3Salidas.put(funcionCol, sem3Salidas.getOrDefault(funcionCol, 0) + 1);
                                }
                            }
                        }
                    }
                }
            }

            for (int i = 0; i < cabeceraBase.length; i++) hojaSemanticaBase.autoSizeColumn(i);

            // =========================================================================
            // 9. NUEVA HOJA: SEMANTICA 2 - TOTAL
            // =========================================================================
            Sheet hojaSemanticaTotal = workbook.createSheet("Semantica 2-Total");
            String[] cabeceraTotal = {"Reglas", "Aparecen", "Aceptada", "Errores"};
            crearFilaCabecera(hojaSemanticaTotal, cabeceraTotal, estiloCabecera);

            // Lista oficial de reglas basada en la plantilla del profesor
            String[] reglasOficiales = {
                "1010", "1011", "1012", "1013", "1020", "1021", "1022", "1030", "1040", "1050", 
                "1060", "1061", "1070", "1080", "1081", "1082", "1083", "1084", "1090", "1100", 
                "1110", "1120", "1130", "1131", "1140", "1150", "1160"
            };

            int filaTotalIndex = 1;
            for (String r : reglasOficiales) {
                Row fila = hojaSemanticaTotal.createRow(filaTotalIndex++);
                
                // Imprimir el número de regla en la columna 0
                fila.createCell(0).setCellValue(Integer.parseInt(r));
                
                // Obtenemos los valores; si no aparecen, automáticamente será 0
                int aparecen = conteoAparecen.getOrDefault(r, 0);
                int aceptadas = conteoAceptadas.getOrDefault(r, 0);
                int errores = conteoErrores.getOrDefault(r, 0);
                
                // Imprimimos siempre, rellenando con ceros donde aplique
                fila.createCell(1).setCellValue(aparecen);
                fila.createCell(2).setCellValue(aceptadas);
                fila.createCell(3).setCellValue(errores);
            }

            for (int i = 0; i < cabeceraTotal.length; i++) hojaSemanticaTotal.autoSizeColumn(i);

           // =========================================================================
            // 10. NUEVA HOJA: SEMANTICA 3 (Resumen de Funciones Nativas)
            // =========================================================================
            Sheet hojaSemantica3 = workbook.createSheet("Semantica 3");
            String[] cabeceraSem3 = {"Función", "Entradas", "Salida", "Aceptados", "Errores"};
            crearFilaCabecera(hojaSemantica3, cabeceraSem3, estiloCabecera);

            int filaSem3Index = 1;
            for (String entidad : todasLasEntidadesSem3) {
                Row fila = hojaSemantica3.createRow(filaSem3Index++);
                fila.createCell(0).setCellValue(entidad);

                // Celda 1: Entradas y Celda 2: Salida
                Cell cellEntradas = fila.createCell(1);
                Cell cellSalidas = fila.createCell(2);
                
                boolean esParametro = entidad.matches(".*_p\\d+");

                if (esParametro) {
                    // ES PARÁMETRO (ej. sqrt_p1)
                    cellEntradas.setCellValue(sem3Entradas.getOrDefault(entidad, 0));
                    cellSalidas.setCellValue(0);

                    // Para los parámetros, los Aceptados y Errores se quedan tal cual como ocurrieron
                    fila.createCell(3).setCellValue(sem3Acept.getOrDefault(entidad, 0));
                    fila.createCell(4).setCellValue(sem3Error.getOrDefault(entidad, 0));
                } else {
                    // ES FUNCIÓN PRINCIPAL (ej. SQRT)
                    int totalSalidas = sem3Salidas.getOrDefault(entidad, 0);
                    cellEntradas.setCellValue(0);
                    cellSalidas.setCellValue(totalSalidas);
                    
                    // REQUERIMIENTO APLICADO: 
                    // Para la función, "Aceptados" siempre es igual a "Salida", y "Errores" siempre es 0.
                    fila.createCell(3).setCellValue(totalSalidas); // Aceptados = Salidas
                    fila.createCell(4).setCellValue(0);            // Errores = 0
                }
            }

            for (int i = 0; i < cabeceraSem3.length; i++) hojaSemantica3.autoSizeColumn(i);

            try (FileOutputStream out = new FileOutputStream(archivo)) {
                workbook.write(out);
            }
            JOptionPane.showMessageDialog(this, "Excel creado exitosamente.");

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error al crear Excel: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}

private static void escribirNumeroOTexto(Cell celda, String valor) {
    try {
        celda.setCellValue(Integer.parseInt(valor.trim()));
    } catch (Exception e) {
        celda.setCellValue(valor);
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

    // =========================================================================
    // CONTEXTOS ANIDADOS (arreglos, llamadas de usuario y funciones nativas)
    // Una sola pila: el tope es siempre la construccion mas interna que esta abierta.
    // Al cerrarse, cada contexto restaura la expresion que habia antes y le agrega
    // UN solo token que la representa (ej. #DArr[1], #DFun(1) o el temporal TD3).
    //
    // Token compuesto: {codigo, lexema, linea, textoOriginal, etiqueta}
    //   etiqueta: "ARR" (elemento de arreglo), "USR" (llamada de usuario),
    //             "NAT" (resultado de funcion nativa), "VOID" (no regresa valor)
    // =========================================================================
    private final Deque<Object> pilaCtx = new ArrayDeque<>();

    class CtxArreglo {
        Identificador arr; String id; Object codigoId; Object linea;
        int dims = 0;
        List<String> dimsTxt = new ArrayList<>();
        List<Object[]> exprPrevia; boolean leyendoPrev; String valorDerPrev;
    }

    class CtxLlamada {
        Identificador func; String nombre; Object codigoId; Object linea;
        int nArgs = 0; boolean errorRegla15 = false;
        List<String> argsTxt = new ArrayList<>();
        List<Object[]> exprPrevia; boolean leyendoPrev; String valorDerPrev;
        List<Object[]> reglasPend = new ArrayList<>();    // 1150/1160 pendientes hasta el ')'
        List<Object[]> erroresPend = new ArrayList<>();
    }

    private static String etiqueta(Object[] t) { return t.length > 4 && t[4] != null ? t[4].toString() : ""; }
    // Texto tal como se escribio en el codigo (ej. "#DArr[1]", "dec(#DA)")
    private static String textoOriginal(Object[] t) { return t.length > 3 && t[3] != null ? t[3].toString() : String.valueOf(t[1]); }
    // Texto que se reporta para un parametro/argumento: las nativas se reportan con su temporal
    private static String textoReporte(Object[] t) { return etiqueta(t).equals("NAT") ? String.valueOf(t[1]) : textoOriginal(t); }

    // Un valor cabe en el destino si es del mismo tipo, si alguno es Variant, o si es un
    // numero "mas chico": Dec cabe en Real y en Exp, y Real cabe en Exp (como la prueba de referencia).
    // Lexema de identificador: empieza con el prefijo de tipo de un id
    private static boolean esIdentificadorLexema(String lex) {
        if (lex == null || lex.isEmpty()) return false;
        char c0 = lex.charAt(0);
        return c0 == '#' || c0 == '@' || c0 == '$' || c0 == '%' || c0 == '¡' || c0 == '¿' || c0 == '&';
    }

    private static boolean cabeEnTipo(int destino, int valor) {
        if (destino == valor || destino == VAR || valor == VAR) return true;
        if (destino == REAL && valor == DEC) return true;
        if (destino == EXP && (valor == DEC || valor == REAL)) return true;
        return false;
    }

    // Lo mismo con los nombres de tipo que usa la tabla de simbolos (Dec, Real, exp...)
    private static boolean cabeEnTipo(String destino, String valor) {
        if (destino.equals(valor) || destino.equals("Variant") || valor.equals("Variant")) return true;
        if (destino.equals("Real") && valor.equals("Dec")) return true;
        if (destino.equalsIgnoreCase("exp") && (valor.equals("Dec") || valor.equals("Real"))) return true;
        return false;
    }

    // Deja las banderas de metodo con los datos de la def abierta mas interna (o vacias si es main)
    private void cargarMetodoActual() {
        CtxMetodo m = pilaMetodos.peek();
        if (m == null) {
            enFuncion = false;
            enProcedimiento = false;
            idMetodoActual = "";
            tipoMetodoActual = "";
            encontroReturn = false;
            lineaMetodoActual = "";
        } else {
            enFuncion = m.esFuncion;
            enProcedimiento = !m.esFuncion;
            idMetodoActual = m.id;
            tipoMetodoActual = m.tipo;
            encontroReturn = m.encontroReturn;
            lineaMetodoActual = m.linea;
        }
    }

    private static String textoDe(List<Object[]> expr) {
        if (expr.size() == 1) return textoOriginal(expr.get(0));
        StringBuilder sb = new StringBuilder();
        for (Object[] t : expr) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(textoOriginal(t));
        }
        return sb.toString();
    }

    // Quita del final de la expresion el id y su "(" o "[" (ya se grabaron como tokens sueltos)
    private static List<Object[]> sinApertura(List<Object[]> expr, String nombre, String abre) {
        List<Object[]> r = new ArrayList<>(expr);
        if (!r.isEmpty() && String.valueOf(r.get(r.size() - 1)[1]).equals(abre)) r.remove(r.size() - 1);
        if (!r.isEmpty() && String.valueOf(r.get(r.size() - 1)[1]).equals(nombre)) r.remove(r.size() - 1);
        return r;
    }

    private static boolean esTemporalNombre(String s) {
        return s != null && s.matches("(TBI|TD|TO|TH|TR|TE|TC|TB|TV)\\d+");
    }

    // Nombre del temporal para el reporte: TD3 -> TDec3, TE1 -> Texp1, etc.
    private static final String[] PREFIJOS_REPORTE = {"TBin", "TDec", "TOct", "THex", "TReal", "Texp", "TCadena", "TBoolean", "TVariant"};
    private static String temporalParaReporte(String s) {
        if (s == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(TBI|TD|TO|TH|TR|TE|TC|TB|TV)(\\d+)$").matcher(s);
        if (!m.matches()) return s;
        for (int k = 0; k < PREFIJOS_TEMP.length; k++) {
            if (PREFIJOS_TEMP[k].equals(m.group(1))) return PREFIJOS_REPORTE[k] + m.group(2);
        }
        return s;
    }

    // Tipo -> "TDec", "Texp", "TVoid"... (columna Tope Pila)
    private static String tipoConT(String tipo) {
        if (tipo == null || tipo.isEmpty()) return "TVariant";
        if (tipo.equalsIgnoreCase("void")) return "TVoid";
        if (tipo.equals("exp")) return "Texp";
        return "T" + tipo;
    }

    private static int codigoDeTipo(String tipo) {
        switch (tipo == null ? "" : tipo) {
            case "Bin": return -61; case "Dec": return -62; case "Oct": return -63; case "Hex": return -64;
            case "Real": return -65; case "exp": return -66; case "Cadena": return -60; case "Boolean": return -67;
            default: return 0;
        }
    }

    class ContextoFuncSem3 {
        String nombreFunc;
        int numParam = 0;
        String tipoParam1 = "";
        boolean p1Valido = false;
        List<String> textosParams = new ArrayList<>();
        boolean todosParamsValidos = true;
        boolean leyendoOpPrevio;
        String valorDerPrevio;
        List<Object[]> exprPrevia;
        Object lineaToken;

        ContextoFuncSem3(String nombreFunc, boolean leyendoOpPrevio, String valorDerPrevio, List<Object[]> exprPrevia, Object lineaToken) {
            this.nombreFunc = nombreFunc;
            this.leyendoOpPrevio = leyendoOpPrevio;
            this.valorDerPrevio = valorDerPrevio;
            this.exprPrevia = new ArrayList<>(exprPrevia);
            this.lineaToken = lineaToken;
        }
    }


    // private void ExportarReglasTXT() {
        // JFileChooser fc = new JFileChooser();
        // fc.setDialogTitle("Exportar Registro de Reglas a TXT");
        // Filtro para que solo permita guardar como .txt
        // fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivo de Texto (*.txt)", "txt"));
        // Nombre por defecto sugerido
        // fc.setSelectedFile(new File("RegistroReglasAvance3-Semantica2-BrandonFornes")); 

        // if (fc.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            // File archivo = fc.getSelectedFile();
            
            // Aseguramos que termine con la extensión .txt
            // if (!archivo.getName().toLowerCase().endsWith(".txt")) {
                // archivo = new File(archivo.getParentFile(), archivo.getName() + ".txt");
            // }

            // try (FileWriter fw = new FileWriter(archivo);
                // BufferedWriter bw = new BufferedWriter(fw);
                // PrintWriter out = new PrintWriter(bw)) {

                // 1. Imprimimos el encabezado de la tabla
                // out.println("=================================================================================================================================================");
                // out.println(String.format("%-8s | %-10s | %-30s | %-60s | %-15s | %-15s", 
                                        // "LÍNEA", "REGLA CVE", "DESCRIPCIÓN", "SITUACIÓN", "LEXEMA", "ÁMBITO"));
                // out.println("=================================================================================================================================================");

                // 2. Recorremos la lista y formateamos cada fila
                // for (Object[] registro : listaRegistroReglas) {
                    // Convertimos cada elemento a String para evitar errores de tipo nulo
                    // String linea = String.valueOf(registro[0]);
                    // String cve = String.valueOf(registro[1]);
                    // String descripcion = String.valueOf(registro[2]);
                    // String situacion = String.valueOf(registro[3]);
                    // String lexema = String.valueOf(registro[4]);
                    // String ambito = String.valueOf(registro[5]);

                    // Imprimimos la fila respetando los mismos anchos de columna del encabezado
                    // out.println(String.format("%-8s | %-10s | %-30s | %-60s | %-15s | %-15s", 
                                            // linea, cve, descripcion, situacion, lexema, ambito));
                // }

                // 3. Pie de página
                // out.println("=================================================================================================================================================");
                // out.println("Total de reglas evaluadas: " + listaRegistroReglas.size());

                // Mensaje de éxito
                // javax.swing.JOptionPane.showMessageDialog(this, "Registro de reglas exportado correctamente.", "Éxito", javax.swing.JOptionPane.INFORMATION_MESSAGE);

            // } catch (Exception e) {
                // Mensaje de error si falla la escritura
                // javax.swing.JOptionPane.showMessageDialog(this, "Error al guardar el archivo: " + e.getMessage(), "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            // }
        // }
    // }
    
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