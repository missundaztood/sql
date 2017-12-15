# SQL エクスポートツール


## 概要

これは、SQL の実行結果をタブ区切りテキストとして出力するツールです。
ファイルに保存した複数の SQL について、実行結果を一度にファイル出力できます。
次のいずれかの方法によって使用できます。

  1. バッチファイル (sql.cmd) に SQL ファイルをドラッグ＆ドロップする
  2. コマンドラインから実行する


## 接続・出力設定

SQL を実行する際には、最低限接続ユーザ・接続先ホストの指定が必要です。
接続・出力に関する設定を次のいずれかの方法によって指定します。

  1. 設定ファイルで指定する
  2. SQL ファイルの先頭にコメントとして設定を記述して指定する
  3. コマンドラインオプションで指定する


## 簡単な使用手順

### 事前準備

  1. [このあたりの説明](https://blogs.oracle.com/dev2dev/get-oracle-jdbc-drivers-and-ucp-from-oracle-maven-repository-without-ides) を
     参考にして settings.xml, settings-security.xml を編集する (m2 配下にテンプレートがあるのでこれをコピーして使うとよい)。

  2. eclipse, またはコンソールで、`mvn package` で sql.jar をビルドして sql.cmd のディレクトリ (このディレクトリ) にコピーする。
  3. sql.conf.template を sql.conf という名前のファイルにコピーする。
  4. 「#user」の行のコメントアウトを解除して (「#」を削除して)「user」を自分のユーザ名に変更する。
  5. 2 と同様に「#host」の行についても「host」を接続先のホスト名・IP アドレスに変更する。
  6. 必要な場合はその他の設定 (password, sid など) についてもコメントアウトを解除して変更する。
  7. バッチファイル (sql.cmd) のショートカットを次のディレクトリに作成する。

    C:\Users\<ユーザ>\AppData\Roaming\Microsoft\Windows\SendTo


### 使用法 (バッチファイルのショートカットを使用)

エクスプローラで SQL ファイルを右クリックして、コンテキストメニューから

  送る > sql.cmd のショートカット

を選択します。SQL が実行され、実行結果が「SQLファイル名-日時.tsv」というファイル
に出力されます。

なお、SQL の末尾は必ず「;」で区切る必要があります (「;」までを 1 つの SQL として
実行します)。

また、デフォルトでは UTF-8 でファイル入出力しますので、(設定で指定しない場合は)
SQL ファイルは UTF-8 で作成する必要があります。

SQL ファイルの先頭にコメントとして設定を記述する方法など、詳細については後述しま
す。


## コマンドラインから実行する場合

### コマンドラインオプションで設定を指定する場合

sql.jar を使用します。引数なしで実行すると、次のように使用法を表示します。

```sh

使用法: java -jar path/to/sql.jar [options] FILE[S...]

  FILE[S...]                        実行する SQL を記述したファイル

FILE に記述された SQL を実行し、タブ区切りのテキストファイルとして出力する。
DB への接続設定は下記の通り。USER, HOST については CONFIG_FILE, または --user オプションによって必ず指定する必要がある。


オプション:

  -c,  --config-file CONFIG_FILE    以下の option で指定可能な設定を書き込んだ CONFIG_FILE を指定する。
  -u,  --user USER                  USER を指定する。CONFIG_FILE で指定しない場合は必ず指定する必要がある。
  -pw, --password PASSWORD          PASSWORD を指定する。省略した場合は USER をパスワードとして使用する。
  -ho, --host HOST                  HOST を指定する。CONFIG_FILE で指定しない場合は必ず指定する必要がある。
  -p,  --port PORT                  PORT を指定する。省略した場合は 1521 を使用する。
  -s,  --sid SID                    SID を指定する。省略した場合は ORCL を使用する。
  -bp, --bind-param NAME=PARAM      PARAM を NAME のバインド変数として設定する。バインドパラメータについては後述。

  -d,  --dir DIR                    出力先の DIR を指定する。
  -rd, --relative-dir DIR           SQL ファイルからの相対パスで出力先の DIR を指定する。
                                    上記 DIR 指定をどちらも省略した場合は FILE と同じディレクトリに出力する。

  -i,  --input-encoding ENCODING    入力エンコーディングとして ENCODING を使用する。省略した場合は UTF-8 を使用する。
  -o,  --output-encoding ENCODING   出力エンコーディングとして ENCODING を使用する。省略した場合は UTF-8 を使用する。
  -so, --stdout-encoding ENCODING   標準出力エンコーディングとして ENCODING を使用する。省略した場合はデフォルトのエン
                                    コーディングを使用する。

  -l,  --list-encodings             (Java で) 指定可能なエンコーディングを表示する。オマケ機能。
  -v,  --verbose                    詳細なメッセージを表示する。
  -h,  --help                       このメッセージを表示する。


使用例:

  「user」というユーザで接続して foo.sql というファイルの SQL を実行する場合:
    java -jar path/to/sql.jar -u user path/to/foo.sql

  sql.conf というファイルの設定で接続して foo.sql というファイルの SQL を実行する場合:
    java -jar path/to/sql.jar -c path/to/sql.conf path/to/foo.sql


SQL ファイルについて:

SQL の末尾には必ず「;」を付与する必要がある (「;」までを 1 つの SQL として実行するため)。
また、SQL ではバインド変数を使用することができる。バインド変数を指定する箇所のプレースホルダは次の 3 つの形式で記述する
ことができる。

  SQL*Plus 風   :foo
  MyBatis 風    #{foo}
  iBATIS 風     #foo#

例えば、次のように SQL を記述できる。

  select * from DEPT d inner join EMP e on d.DEPTNO = e.DEPTNO
  where d.DNAME like :dname
    and e.SAL between :bottom and :top
    and e.HIREDATE between :begin and :end;

上記の SQL は次のようにバインド変数を指定して実行できる。

  java -jar sql.jar -bp dname=SALES -bp bottom=1000 -bp top=2000 -bp begin=1981/02/01 -bp end=1981/02/28 emp.sql

```


例えば user というユーザで接続して foo.sql というファイルに含まれる SQL を実行す
る場合は次のようにコマンドを実行します。

  java -jar path\to\sql.jar -u user path/to/foo.sql


このとき、「-u user」の指定は「--user user」とすることもできます。


#### 補足

上記のコマンドを cygwin から実行すると出力結果が文字化けしてしまいます。文字化け
を回避するためのシェルスクリプト (sql.sh) を用意していますので、cygwin から実行
する場合はこちらを使用できます。


### 設定ファイルで設定を指定する場合

sql.conf というファイルに設定を記述している場合、例えば次のように実行できます。

  java -jar path\to\sql.jar -c path/to/sql.conf path/to/foo.sql


設定ファイルは、タブ区切りで次のように記述します。

設定項目	設定値


sql.conf.template というファイルが設定ファイルの記載例となります。適切な名前のファ
イルに (sql.conf など) コピーして使用してください。

設定ファイルでは「#」で始まる行はコメントとみなします。記載例のファイルでは全て
コメントアウトしてあります。


### SQL ファイルの先頭にコメントとして設定を記述する場合

サンプルとして sample.sql というファイルを用意しています。

記載方法は設定ファイルの場合と基本的に同様です。「--」を行頭に付与付与して次のよ
うに記述します。

--設定項目	設定値


なお、SQL ファイルを読み取って設定をロードする都合上、「input-encoding」は指定で
きません。


## バッチファイル (sql.cmd) に SQL ファイルをドラッグ＆ドロップして実行する場合

「簡単な使用手順」に記載した通りです。

なお、バッチファイルへのドラッグ＆ドロップで実行する場合は、バッチファイルが配置
されているディレクトリに「sql.conf」という名前で設定ファイルを配置しておく必要が
あります。
