rm -rf target/release
mkdir target/release
cd target/release
git clone git@github.com:burtbeckwith/grails-nettymvc.git
cd grails-nettymvc
grails clean
grails compile
#grails publish-plugin --noScm --snapshot --stacktrace
grails publish-plugin --noScm --stacktrace
