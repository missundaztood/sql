import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Sql {

    private static String[] usages = {
            "使用法: java -jar path/to/sql.jar [options] FILE[S...]",
            "",
            "  FILE[S...]                        実行する SQL を記述したファイル",
            "",
            "FILE に記述された SQL を実行し、タブ区切りのテキストファイルとして出力する。",
            "DB への接続設定は下記の通り。USER, HOST については CONFIG_FILE, または --user オプションによって必ず指定する必要がある。",
            "",
            "",
            "オプション:",
            "",
            "  -c,  --config-file CONFIG_FILE    以下の option で指定可能な設定を書き込んだ CONFIG_FILE を指定する。",
            "  -u,  --user USER                  USER を指定する。CONFIG_FILE で指定しない場合は必ず指定する必要がある。",
            "  -pw, --password PASSWORD          PASSWORD を指定する。省略した場合は USER をパスワードとして使用する。",
            "  -ho, --host HOST                  HOST を指定する。CONFIG_FILE で指定しない場合は必ず指定する必要がある。",
            "  -p,  --port PORT                  PORT を指定する。省略した場合は 1521 を使用する。",
            "  -s,  --sid SID                    SID を指定する。省略した場合は ORCL を使用する。",
            "  -bp, --bind-param NAME=PARAM      PARAM を NAME のバインド変数として設定する。バインドパラメータについては後述。",
            "",
            "  -d,  --dir DIR                    出力先の DIR を指定する。",
            "  -rd, --relative-dir DIR           SQL ファイルからの相対パスで出力先の DIR を指定する。",
            "                                    上記 DIR 指定をどちらも省略した場合は FILE と同じディレクトリに出力する。",
            "",
            "  -i,  --input-encoding ENCODING    入力エンコーディングとして ENCODING を使用する。省略した場合は UTF-8 を使用する。",
            "  -o,  --output-encoding ENCODING   出力エンコーディングとして ENCODING を使用する。省略した場合は UTF-8 を使用する。",
            "  -so, --stdout-encoding ENCODING   標準出力エンコーディングとして ENCODING を使用する。省略した場合はデフォルトのエン",
            "                                    コーディングを使用する。",
            "",
            "  -l,  --list-encodings             (Java で) 指定可能なエンコーディングを表示する。オマケ機能。",
            "  -v,  --verbose                    詳細なメッセージを表示する。",
            "  -h,  --help                       このメッセージを表示する。",
            "",
            "",
            "使用例:",
            "",
            "  「user」というユーザで接続して foo.sql というファイルの SQL を実行する場合:",
            "    java -jar path/to/sql.jar -u user path/to/foo.sql",
            "",
            "  sql.conf というファイルの設定で接続して foo.sql というファイルの SQL を実行する場合:",
            "    java -jar path/to/sql.jar -c path/to/sql.conf path/to/foo.sql",
            "",
            "",
            "SQL ファイルについて:",
            "",
            "SQL の末尾には必ず「;」を付与する必要がある (「;」までを 1 つの SQL として実行するため)。",
            "また、SQL ではバインド変数を使用することができる。バインド変数を指定する箇所のプレースホルダは次の 3 つの形式で記述する",
            "ことができる。",
            "",
            "  SQL*Plus 風   :foo",
            "  MyBatis 風    #{foo}",
            "  iBATIS 風     #foo#",
            "",
            "例えば、次のように SQL を記述できる。",
            "",
            "  select * from DEPT d inner join EMP e on d.DEPTNO = e.DEPTNO",
            "  where d.DNAME like :dname",
            "    and e.SAL between :bottom and :top",
            "    and e.HIREDATE between :begin and :end;",
            "",
            "上記の SQL は次のようにバインド変数を指定して実行できる。",
            "",
            "  java -jar sql.jar -bp dname=SALES -bp bottom=1000 -bp top=2000 -bp begin=1981/02/01 -bp end=1981/02/28 emp.sql",
            "",
    };

    private static void usage(int code, String... messages) {
        if (messages.length > 0) {
            for (String message: messages)
                stdout.printf("%s\n", message);
            stdout.print("\n");
        }
        for (String message: usages)
            stdout.printf("%s\n", message);
        System.exit(code);
    }

    private static void listEncodings() {
        for (Charset cs: Charset.availableCharsets().values())
            stdout.printf("%s (%s)\n", cs, cs.aliases());
        System.exit(0);
    }

    private static PrintStream stdout = System.out;

    private static void setOutputConsoleEncoding(String encoding) throws UnsupportedEncodingException {
        stdout = new PrintStream(System.out, false, encoding);
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0)
            usage(0);
        Sql sql = new Sql();
        int index = 0;
        optionsLoop: for (; index < args.length; ++index) {
            switch (args[index]) {
            case "-c":  case "--config-file": sql.loadConfiguration(args[++index]); break;
            case "-u":  case "--user": sql.user = args[++index]; break;
            case "-pw": case "--password": sql.password = args[++index]; break;
            case "-ho": case "--host": sql.host = args[++index]; break;
            case "-p":  case "--port": sql.port = args[++index]; break;
            case "-s":  case "--sid": sql.sid = args[++index]; break;
            case "-bp":  case "--bind-param": sql.setBindParam(args[++index]); break;

            case "-d":  case "--dir": sql.setDir(args[++index]); break;
            case "-rd": case "--relative-dir": sql.relativeDir = args[++index]; break;

            case "-i":  case "--input-encoding": sql.setInputEncoding(args[++index]); break;
            case "-o":  case "--output-encoding": sql.setOutputEncoding(args[++index]); break;
            case "-so": case "--stdout-encoding": setOutputConsoleEncoding(args[++index]); break;

            case "-l":  case "--list-encodings": listEncodings();
            case "-v":  case "--verbose": sql.isVerbose = true; break;
            case "-h":  case "--help": usage(0);

            default: break optionsLoop;
            }
        }
        if (sql.isVerbose)
            for (int i = 0; i < args.length; ++i)
                stdout.printf("%d: %s\n", i, args[i]);
        if (sql.isVerbose)
            stdout.printf("%s\n", sql);
        Class.forName("oracle.jdbc.driver.OracleDriver");
        for (; index < args.length; ++index)
            sql.executeSql(Paths.get(args[index]));
        sql.disconnect();
    }

    private Connection conn;
    private String user;
    private String password;
    private String host;
    private String port = "1521";
    private String sid = "ORCL";
    private Map<String, Object> bindParams = new LinkedHashMap<>();
    private Path dir;
    private String relativeDir;
    private Charset inputEncoding = Charset.forName("UTF-8");
    private Charset outputEncoding = Charset.forName("UTF-8");
    private boolean isVerbose;

    @Override
    public String toString() {
        return String.format(
                "Sql [conn=%s, user=%s, password=%s, host=%s, port=%s, sid=%s, dir=%s, inputEncoding=%s, outputEncoding=%s, isVerbose=%s]",
                conn, user, password, host, port, sid, dir, inputEncoding, outputEncoding, isVerbose);
    }

    void loadConfiguration(String configFile) throws IOException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, SecurityException, NoSuchFieldException
    {
        try (BufferedReader in = Files.newBufferedReader(Paths.get(configFile), inputEncoding)) {
            for (String line; (line = in.readLine()) != null; ) {
                Matcher commentMr = Pattern.compile("^([^#]+?)\\s*+#.*$").matcher(line);
                if (commentMr.matches())
                    line = commentMr.group(1);
                Matcher confMr = Pattern.compile("^([\\w-]+)\t(.+?)\\s*+$").matcher(line);
                if (confMr.matches())
                    setProperty(confMr.group(1), confMr.group(2));
            }
        }
    }

    private void setProperty(String option, String value)
            throws IllegalAccessException, InvocationTargetException, NoSuchFieldException {
        String property = toCamelCase(option);
        String setter = String.format("set%s%s", property.substring(0, 1).toUpperCase(), property.substring(1));
        Class<? extends Sql> klass = getClass();
        try {
            Method method = klass.getDeclaredMethod(setter, String.class);
            method.invoke(this, value);
            if (isVerbose)
                stdout.printf("%s: %s\n", setter, value);
        } catch (NoSuchMethodException e) {
            Field field = klass.getDeclaredField(property);
            field.set(this, value);
            if (isVerbose)
                stdout.printf("%s: %s\n", property, value);
        }
    }

    private static final Pattern chainPt = Pattern.compile("-([\\w])");

    private String toCamelCase(String chainCaseName) {
        Matcher chainMr = chainPt.matcher(chainCaseName);
        StringBuffer buff = new StringBuffer();
        while (chainMr.find())
            chainMr.appendReplacement(buff, chainMr.group(1).toUpperCase());
        chainMr.appendTail(buff);
        return buff.toString();
    }

    void setUser(String user) throws SQLException {
        this.user = user;
        if (password == null)
            password = user;
        disconnect();
    }

    void setPassword(String password) throws SQLException {
        this.password = password;
        disconnect();
    }

    void setHost(String host) throws SQLException {
        this.host = host;
        disconnect();
    }

    void setPort(String port) throws SQLException {
        this.port = port;
        disconnect();
    }

    void setSid(String sid) throws SQLException {
        this.sid = sid;
        disconnect();
    }

    void setDir(String dir) {
        this.dir = Paths.get(dir);
    }

    void setInputEncoding(String encoding) {
        inputEncoding = Charset.forName(encoding);
    }

    void setOutputEncoding(String encoding) {
        outputEncoding = Charset.forName(encoding);
    }

    private static final Pattern bindParamDefPt;
    private static final Pattern bindParamPlaceHolderPt;

    static {
        String sqlplusNameClass = "\\w-";
        String batisNameClass = "\\w-.";
        bindParamDefPt = Pattern.compile(String.format("^([%s]+)=(.*?)(:(int|BigDecimal|Timestamp|Timestamp\\(.*\\)))?$", batisNameClass));
        bindParamPlaceHolderPt = Pattern.compile(String.format(":([%s]+)|#\\{([%s]+)\\}|#([%s]+)#", sqlplusNameClass, batisNameClass, batisNameClass));
    }

    private void setBindParam(String param) throws ParseException {
        Matcher bindparamDefMr = bindParamDefPt.matcher(param);
        if (bindparamDefMr.matches()) {
            String name = bindparamDefMr.group(1);
            String value = bindparamDefMr.group(2);
            String type = bindparamDefMr.group(4);
            bindParams.put(name, type == null ? value : convert(type, value, bindparamDefMr.start(4)));
        }
    }

    private Object convert(String type, String value, int pos) throws ParseException {
        switch (type) {
        case "int": return Integer.valueOf(value);
        case "BigDecimal": return new BigDecimal(value);
        case "Timestamp": return Timestamp.valueOf(value);
        default:
            Matcher dateFormatMr = Pattern.compile("^Timestamp\\((.*)\\)$").matcher(type);
            if (dateFormatMr.matches())
                return new Timestamp(new SimpleDateFormat(dateFormatMr.group(1)).parse(value).getTime());
            else
                throw new ParseException(String.format("[%s] という型は無効です。", type), pos);
        }
    }

    private void connect() throws SQLException {
        conn = DriverManager.getConnection(
                String.format("jdbc:oracle:thin:@%s:%s:%s", host, port, sid), user, password);
        conn.setAutoCommit(false);
    }

    private void disconnect() throws SQLException {
        if (conn == null)
            return;
        conn.close();
        conn = null;
    }

    private static final Pattern confPt = Pattern.compile("^--\\s*([\\w-]+)\t(.+?)\\s*+$");
    private static final Pattern eosPt = Pattern.compile("^([^;]*)(;.*)$");

    void executeSql(Path inPath) throws ClassNotFoundException,
            SQLException, IOException, IllegalAccessException, InvocationTargetException, NoSuchFieldException
    {
        try (BufferedReader in = Files.newBufferedReader(inPath, inputEncoding)) {
            StringBuilder buff = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                Matcher confMr = confPt.matcher(line);
                if (!confMr.matches())
                    break;
                setProperty(confMr.group(1), confMr.group(2));
            }
            if (line == null)
                return;
            if (conn == null)
                connect();
            Path outPath = getOutDir(inPath).resolve(toOutputFilename(inPath));
            try (PrintStream out = new PrintStream(new BufferedOutputStream(
                    Files.newOutputStream(outPath)), false, outputEncoding.displayName()))
            {
                if (bindParams.size() > 0) {
                    if (isVerbose)
                        stdout.printf("bind parameters:\n");
                    out.printf("bind parameters:\n");
                    for (Map.Entry<String, Object> entry: bindParams.entrySet()) {
                        if (isVerbose)
                            stdout.printf("%s: [%s]\n", entry.getKey(), entry.getValue());
                        out.printf("%s: [%s]\n", entry.getKey(), entry.getValue());
                    }
                }
                do {
                    Matcher eosMr = eosPt.matcher(line);
                    if (!eosMr.matches()) {
                        buff.append(line).append("\n");
                        continue;
                    }
                    String sql = buff.append(eosMr.group(1)).toString();
                    String readableSql = buff.append(eosMr.group(2)).toString();
                    buff.setLength(0);
                    stdout.printf("%s\n", readableSql);
                    out.printf("\n%s\n", repeat("-", 80));
                    out.printf("%s\n\n", readableSql.replaceFirst("(?s)^\\s*", ""));
                    export(out, sql);
                } while ((line = in.readLine()) != null);
            }
        }
    }

    private Path getOutDir(Path inPath) throws IOException {
        Path outDir =
                relativeDir != null ? inPath.getParent().resolve(relativeDir) :
                dir != null ? dir :
                inPath.getParent();
        if (!Files.isDirectory(outDir))
            Files.createDirectories(outDir);
        return outDir;
    }

    private String toOutputFilename(Path inPath) {
        String filename = inPath.getFileName().toString();
        Matcher mr = Pattern.compile("^(.+)\\.(.+)$").matcher(filename);
        if (mr.matches())
            filename = mr.group(1);
        return String.format("%s-%s.tsv", filename, new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()));
    }

    private String repeat(String str, int times) {
        return new String(new char[times]).replace("\0", str);
    }

    private void export(PrintStream out, String sql) throws SQLException {
        List<String> names = new ArrayList<>();
        String converted;
        {
            Matcher mr = bindParamPlaceHolderPt.matcher(sql);
            StringBuffer buff = new StringBuffer();
            while (mr.find()) {
                String name = mr.group(1) != null ? mr.group(1) :
                    mr.group(2) != null ? mr.group(2) :
                    mr.group(3);
                names.add(name);
                mr.appendReplacement(buff, "?");
            }
            mr.appendTail(buff);
            converted = buff.toString();
        }
        if (isVerbose)
            stdout.printf("%s\n", converted);
        PreparedStatement statement = conn.prepareStatement(converted);
        for (int i = 0, len = names.size(); i < len; ++i) {
            String name = names.get(i);
            Object value = bindParams.get(name);
            statement.setObject(i + 1, value);
            if (isVerbose)
                stdout.printf("%s: %s%s", name, value, i == len - 1 ? "\n" : ", ");
        }
        ResultSet result = statement.executeQuery();
        int r;
        for (r = 1; result.next(); ++r) {
            ResultSetMetaData meta = result.getMetaData();
            int columnCount = meta.getColumnCount();
            if (r == 1) {
                for (int c = 1; c < columnCount; ++c) {
                    out.print(meta.getColumnName(c));
                    if (c < columnCount)
                        out.print("\t");
                }
                out.print("\n\n");
            }
            for (int c = 1; c <= columnCount; ++c) {
                Object value = result.getObject(c);
                out.print(value == null ? "" : value);
                if (c < columnCount)
                    out.print("\t");
            }
            out.print("\n");
        }
        if (isVerbose)
            stdout.printf("count: %s\n", r - 1);
    }
}
