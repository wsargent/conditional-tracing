package com.tersesystems.dynamicspans;

import java.io.Reader;

interface ConditionSource {
    public boolean isInvalid();

    public Reader getReader();
}
