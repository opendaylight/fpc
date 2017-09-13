cd karaf/target/assembly/bin
if [ "$1" = "debug" ]
then
	./karaf debug
else
	./karaf
fi
