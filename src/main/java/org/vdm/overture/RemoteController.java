package org.vdm.overture;

import org.overture.interpreter.debug.RemoteControl;
import org.overture.interpreter.debug.RemoteInterpreter;

public class RemoteController implements RemoteControl {
    public static RemoteInterpreter interpreter = null;

    public void run(RemoteInterpreter interpreter) throws Exception {
        RemoteController.interpreter = interpreter;
    }
}
