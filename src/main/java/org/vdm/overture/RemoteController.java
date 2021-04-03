package org.vdm.overture;

import org.overture.interpreter.debug.RemoteControl;
import org.overture.interpreter.debug.RemoteInterpreter;

public class RemoteController implements RemoteControl {
    public static RemoteInterpreter interpreter = null;

    public void run(RemoteInterpreter interpreter) throws Exception {
        System.out.println("VDM remote interpreter initialized!");
        RemoteController.interpreter = interpreter;
        String mainClass = System.getProperty("mainClass");
        if (mainClass == null) {
            mainClass = System.getenv("mainClass");
        }
        if (mainClass != null) {
            try {
                Class.forName(mainClass).getMethod("main", String[].class).invoke(null, new Object[] { new String[0] });
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        } else {
            System.out.println("Error: Environment variable 'mainClass' not provided!");
        }
        interpreter.finish();
    }
}
