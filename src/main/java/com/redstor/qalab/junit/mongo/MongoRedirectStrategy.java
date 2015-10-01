package com.redstor.qalab.junit.mongo;

public enum MongoRedirectStrategy {
    /**
     * Do not redirect stdout and stderr
     */
    None,
    /**
     * Redirect stdout and stderr to separate streams
     */
    Split,
    /**
     * Redirect stdout and stderr to a single stream
     */
    Combine
}
