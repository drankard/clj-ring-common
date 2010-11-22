# Wrappers for ring  

Common wrappers for ring:

### wrap-promote-header
Adds all the headers as lowercase and prefixed with 'header_' to the params var.
This enables using headers a function arguments in routes.


### wrap-request-log-and-error-handling 
Logs the request and response with contrib.logging as INFO 
Catches all the exception thrown and returns a json-object with the exception message. If java.lang.Exception a httpcode of 500 is used. If AssertionError - an custom httpcode is used, if the asseration Exception msg contains a pre or post condition with athe chk function.  

The chk function, is used in pre and post condition for setting the error-code.

## Installation 
in project.clj under dependencies add 

[common-ring "1.0.0-SNAPSHOT"]

The jar is located at clojars.org

## Usage

in the source file import the following namespace ring.commonrest

## License

Copyright (C) 2010 

Distributed under the Eclipse Public License, the same as Clojure.
