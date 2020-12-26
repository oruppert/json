sources = $(wildcard *.java)
classes = out
jarfile = json.jar
main-class = Test

all: $(jarfile)

clean:
	rm -rf $(classes) $(jarfile)

$(jarfile): $(sources)
	mkdir -p $(classes)
	javac -d $(classes) $(sources)
	jar cef $(main-class) $(jarfile) -C $(classes) .

test: $(jarfile)
	java -jar $(jarfile)
