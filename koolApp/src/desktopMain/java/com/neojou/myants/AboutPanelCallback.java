package com.neojou.myants;

import org.lwjgl.system.Callback;
import org.lwjgl.system.CallbackI;
import org.lwjgl.system.libffi.FFICIF;

import static org.lwjgl.system.APIUtil.apiCreateCIF;
import static org.lwjgl.system.libffi.LibFFI.FFI_DEFAULT_ABI;
import static org.lwjgl.system.libffi.LibFFI.ffi_type_pointer;
import static org.lwjgl.system.libffi.LibFFI.ffi_type_void;

/** ObjC method `void showAbout:(id)sender`. Kept alive by the caller. */
public final class AboutPanelCallback extends Callback implements CallbackI {
    private static final FFICIF CIF = apiCreateCIF(
        FFI_DEFAULT_ABI,
        ffi_type_void,
        ffi_type_pointer,
        ffi_type_pointer,
        ffi_type_pointer
    );

    private final Runnable action;

    public AboutPanelCallback(Runnable action) {
        super(CIF);
        this.action = action;
    }

    @Override
    public FFICIF getCallInterface() {
        return CIF;
    }

    @Override
    public void callback(long ret, long args) {
        action.run();
    }
}
