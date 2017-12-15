#!/usr/bin/bash

main() {
    basedir=`dirname "$0" | xargs cygpath -w`
    if [ $# -eq 0 ]; then
        java -jar "$basedir"/sql.jar 2>&1 | perl -Mopen=':std,IN,:encoding(cp932),OUT,:utf8' -pe ''
    else
        perl -e 'for my $arg (@ARGV) { if (-e $arg) { system qq[cygpath -w "$arg"] } else { print "$arg\n" } }' -- "$@" | \
            perl -pe 's/^.*$/"$&"/' | \
            xargs java -jar "$basedir"/sql.jar 2>&1 | perl -Mopen=':std,IN,:encoding(cp932),OUT,:utf8' -pe ''
    fi
}

main "$@"
