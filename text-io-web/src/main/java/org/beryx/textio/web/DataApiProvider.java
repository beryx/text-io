package org.beryx.textio.web;

public interface DataApiProvider<CTX> {
    DataApi create(CTX ctx, String initData);
    DataApi get(CTX ctx);
}
