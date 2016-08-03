# vscode-arcadia

### Getting Started
To try it out, clone the repo somewhere & open the 
root folder in VSCode.  Run the extension in an 
Extension Development Host by pressing `F5`, or 
clicking `Debug` > `Start`.  This runs the compiled
extension.js file in the root folder.

Ensure Unity is up & an Arcadia project is loaded.
Then `⌘-shift-p` and run `Arcadia: REPL - Start`.
If you have an open Clojure file you want to evaluate
then `Arcadia: REPL - Send File` 
(`ctrl+, f`) will activate the REPL and eval 
the whole file at the same time.

Once the REPL is active, you can `Arcadia: REPL - Send Line`
(`ctrl+, l`) or `Arcadia: REPL - Send Selection` 
(`ctrl+, s`) to send lines or the current selection to the REPL.

There is currently no in-REPL editing available, but you can
open an empty file & use it as a scratch buffer.  The REPL
doesn't care what syntax the file is set to, but you would
need to set it Clojure to get syntax highlighting obviously.

### Development 
To make changes or just build fresh from source,
run `lein cljsbuild once` (or `auto` for continuous
compilation);