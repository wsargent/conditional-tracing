package com.tersesystems.conditionalspans;

import java.io.Reader;

interface ConditionSource {
    public boolean isInvalid();

    public Reader getReader();
}
