package com.bugsnag.android;

class Stacktrace implements JsonStream.Streamable {
    Configuration config;
    StackTraceElement[] stacktrace;

    Stacktrace(Configuration config, StackTraceElement[] stacktrace) {
        this.config = config;
        this.stacktrace = stacktrace;
    }

    public void toStream(JsonStream writer) {
        writer.array();

        for(StackTraceElement el : stacktrace) {
            try {
                writer.object()
                    .name("method").value(el.getClassName() + "." + el.getMethodName())
                    .name("file").value(el.getFileName() == null ? "Unknown" : el.getFileName())
                    .name("lineNumber").value(el.getLineNumber());

                if(config.classInProject(el.getClassName())) {
                    writer.name("inProject").value(true);
                }

                writer.endObject();
            } catch(Exception lineEx) {
                lineEx.printStackTrace(System.err);
            }
        }

        writer.endArray();
    }
}
