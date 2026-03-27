package com.example;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.Color;
import java.awt.Font;
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
    private static String[][] matriz;
    private int[] categorias = new int[32];
    private final int ERRORES                        = 0;
    private final int ID_CADENA                       = 1;
    private final int ID_NUMERICA_BINARIO             = 2;
    private final int ID_NUMERICA_DECIMAL             = 3;
    private final int ID_NUMERICA_OCTAL               = 4;
    private final int ID_NUMERICA_HEXADECIMAL         = 5;
    private final int ID_REAL                         = 6;
    private final int ID_EXPONENCIAL                  = 7;
    private final int ID_BOOLEANAS                    = 8;
    private final int COMENTARIOS                     = 9;
    private final int PALABRAS_RESERVADAS             = 10;
    private final int CONSTANTES_CADENA               = 11;
    private final int CONSTANTES_NUMERICA_BINARIO     = 12;
    private final int CONSTANTES_NUMERICA_DECIMAL     = 13;
    private final int CONSTANTES_NUMERICA_OCTAL       = 14;
    private final int CONSTANTES_NUMERICA_HEXADECIMAL = 15;
    private final int CONSTANTES_REAL                 = 16;
    private final int CONSTANTES_EXPONENCIAL          = 17;
    private final int CONSTANTES_BOOLEANAS            = 18;
    private final int CONSTANTES_NULA                 = 19;
    private final int OPERADORES_POSTFIX              = 20;
    private final int OPERADORES_LOGICOS_BINARIOS     = 21;
    private final int OPERADORES_CONTROL              = 22;
    private final int OPERADORES_MATEMATICOS          = 23;
    private final int OPERADOR_EXPONENTE              = 24;
    private final int OPERADORES_TURNO                = 25;
    private final int OPERADORES_RELACIONALES         = 26;
    private final int OPERADORES_SIN_IGUALDAD         = 27;
    private final int OPERADORES_LOGICOS              = 28;
    private final int OPERADOR_TERNARIO               = 29;
    private final int OPERADORES_ASIGNACION           = 30;
    private final int OPERADORES_AGRUPAMIENTO         = 31;
    
    private final String[] nombresGrupos = {
    "ERRORES", "ID_CADENA", "ID_NUMERICA_BINARIO", "ID_NUMERICA_DECIMAL",
    "ID_NUMERICA_OCTAL", "ID_NUMERICA_HEXADECIMAL", "ID_REAL", "ID_EXPONENCIAL",
    "ID_BOOLEANAS", "COMENTARIOS", "PALABRAS_RESERVADAS", "CONSTANTES_CADENA",
    "CONSTANTES_NUMERICA_BINARIO", "CONSTANTES_NUMERICA_DECIMAL", "CONSTANTES_NUMERICA_OCTAL",
    "CONSTANTES_NUMERICA_HEXADECIMAL", "CONSTANTES_REAL", "CONSTANTES_EXPONENCIAL",
    "CONSTANTES_BOOLEANAS", "CONSTANTES_NULA", "OPERADORES_POSTFIX",
    "OPERADORES_LOGICOS_BINARIOS", "OPERADORES_CONTROL", "OPERADORES_MATEMATICOS",
    "OPERADOR_EXPONENTE", "OPERADORES_TURNO", "OPERADORES_RELACIONALES",
    "OPERADORES_SIN_IGUALDAD", "OPERADORES_LOGICOS", "OPERADOR_TERNARIO",
    "OPERADORES_ASIGNACION", "OPERADORES_AGRUPAMIENTO"
    };
    

    ArrayList<String> palabrasReservadas = new ArrayList<>(Arrays.asList(
        "if", "else", "switch", "for", "do", "while", "console.log", "forEach", "break", "continue", "let", "const",
        "undefined", "interface", "typeof", "any", "interface", "set", "get", "class", "toLowerCase",
        "toUpperCase", "length", "trim", "charAt", "startsWith", "endsWith", "indexOf", "Includes","slice",
        "replace", "split", "push", "shift", "in", "of", "splice", "concat", "find", "findIndex", "filter", "map", "sort",
        "reverse"
    ));
    ArrayList<String> booleanas = new ArrayList<>(Arrays.asList(
        "true", "false"
    ));
    ArrayList<String> nulas = new ArrayList<>(Arrays.asList(
        "null"
    ));

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
    put("console.log", -78);
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
            case -1,-5,-8,-11,-21:
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
            case -72,-73,-74,-75,-76,-77,-78,-79,-80,-81,-82,-83,-84,-85,-86,
                 -87,-88,-89,-90,-91,-92,-93,-94,-95,-96,-97,-98,-99,
                 -100, -101, -102, -103, -104, -105, -106, -107, -108,
                 -109, -110, -111, -112, -113, -114:
                categorias[PALABRAS_RESERVADAS]++;
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
        return 510;
    }

    public static String[][] leerRangoCSV(String archivo, int fIni, int fFin, int cIni, int cFin) throws IOException {
        List<String[]> lineasFiltradas = new ArrayList<>();
        //System.out.println("El programa está buscando en: " + System.getProperty("user.dir"));
        try {
            InputStream is = AnalizadorLexico.class.getClassLoader().getResourceAsStream("MATRIZ_CSV.csv");
        
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

        fileNameLabel = new JLabel(" Sin archivo ");
        fileNameLabel.setForeground(TEXT_MAIN);

        bar.add(btnAbrir);
        bar.add(btnCompilar);
        bar.add(btnXLS);
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
        return p;
    }

    private JPanel buildRightPanel() {
        JPanel p = new JPanel(new BorderLayout());
        
        JPanel panelErrores = new JPanel(new BorderLayout());
        JLabel lblErr = new JLabel("ERRORES LÉXICOS");
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
        actualizarListaErrores();
        
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
            llenarDatosHoja(hojaErrores, listaErrores);

            //CONTADORES
            Sheet hojaCategorias = workbook.createSheet("CONTADORES");

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

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        try {
            matriz = leerRangoCSV("MATRIZ_CSV.csv", 2, 93, 2, 51);
            // System.out.println("Primer valor cargado: " + miMatriz[0][0]);
            //System.out.println(matriz[91][28]);
        } catch (IOException e) {
            System.err.println("Error: No se encontró el archivo matriz.");
        }
        SwingUtilities.invokeLater(AnalizadorLexico::new);
    }
}