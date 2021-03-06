package com.swmansion.reanimated.bridging;

import com.facebook.react.bridge.JSApplicationIllegalArgumentException;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactContext;
import com.swmansion.reanimated.NodesManager;
import com.swmansion.reanimated.nodes.Node;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import javax.annotation.Nullable;

import static com.swmansion.reanimated.Utils.concat;

class ReactMethodAccessor extends NativeModuleAccessor implements ReanimatedBridge.ReanimatedAccessor {
    private NativeModule mCallee;
    private MethodAccessor mMethod;

    ReactMethodAccessor(ReactContext context, String moduleName, String methodName) {
        super(context);
        setCaller(moduleName, methodName);
    }

    private void setCaller(String moduleName, String methodName) {
        NativeModule module = getNativeModule(moduleName);
        setCaller(module, methodName);
    }

    private void setCaller(NativeModule module, String methodName){
        mCallee = module;
        mMethod = getReactMethod(module, methodName);
    }

    public void call(int[] params, NodesManager nodesManager) {
        final Object[] args = cast(params, nodesManager);
        nodesManager
                .getContext()
                .runOnNativeModulesQueueThread(
                        new Runnable() {
                            @Override
                            public void run() {
                                invoke(args);
                            }
                        });

    }

    private void invoke(Object[] params) {
        try {
            mMethod.getMethod().invoke(mCallee, params);
        } catch (Throwable err) {
            String errorMessage;
            if(err instanceof InvocationTargetException){
                errorMessage = err.getCause().getMessage();
            } else {
                errorMessage = err.getMessage();
            }

            throw new JSApplicationIllegalArgumentException(
                    "Reanimated invoke error\n" +
                            String.format("module: %s,\n", mCallee.getName()) +
                            String.format("method: %s,\n", mMethod.getName()) +
                            String.format("\nparams:\n%s\n\n", concat(params, "\n")) +
                            "Details: " + errorMessage,
                    err
            );
        }
    }

    private Object[] cast(int[] params, NodesManager nodesManager) {
        Object[] out = new Object[params.length];
        Node n;
        Object value;
        Class<?>[] paramTypes = mMethod.getParamTypes();
        Class<?> paramType;
        int k = 0;

        try {
            for (k = 0; k < params.length; k++) {
                paramType = paramTypes[k];
                n = nodesManager.findNodeById(params[k], Node.class);
                value = n.value();

                if (value != null && BridgingUtils.isNumber(value)) {
                    out[k] = BridgingUtils.fromDouble(((Double) value), paramType);
                } else {
                    out[k] = paramType.cast(value);
                }
            }
        } catch (Throwable err){
            String[] inputTypes = new String[params.length];
            ArrayList<String> parts = new ArrayList<>();
            @Nullable Object tempVal;
            for (int i = 0; i < params.length; i++) {
                n = nodesManager.findNodeById(params[i], Node.class);
                tempVal = n.value();
                parts.clear();
                parts.add(paramTypes[i].getSimpleName());
                parts.add(n.getClass().getSimpleName());
                parts.add(tempVal == null ? "null" : tempVal.toString());
                inputTypes[i] = concat(parts.toArray());
            }

            throw new JSApplicationIllegalArgumentException(
                    String.format("Reanimated invoke error\nParameter #%d mismatch\n", k + 1) +
                            String.format("module: %s,\n", mCallee.getName()) +
                            String.format("method: %s,\n", mMethod.getName()) +
                            (err instanceof ArrayIndexOutOfBoundsException ? String.format("Expected %d params, got %d\n", paramTypes.length, params.length) : "") +
                            String.format("\nArgs (expected, got, value):\n\n%s\n\n", concat(inputTypes, "\n")),
                    err
            );
        }

        return out;
    }

    @Override
    public void connectToView(int viewTag) {
        //  noop
        //  this is used for invoking a view manager command only
    }

    @Override
    public void disconnectFromView(int viewTag) {
        //  noop
        //  this is used for invoking a view manager command only
    }
}