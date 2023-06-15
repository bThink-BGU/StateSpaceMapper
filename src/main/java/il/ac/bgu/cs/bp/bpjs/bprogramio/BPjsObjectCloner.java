package il.ac.bgu.cs.bp.bpjs.bprogramio;

import il.ac.bgu.cs.bp.bpjs.BPjs;
import il.ac.bgu.cs.bp.bpjs.execution.jsproxy.BProgramJsProxy;
import il.ac.bgu.cs.bp.bpjs.model.BProgram;
import org.mozilla.javascript.Context;

import java.io.*;

public class BPjsObjectCloner {
  private BProgram bprogram;
  private StubProvider stubProvider = null;

  public BPjsObjectCloner(BProgram bprogram) {
    this.bprogram = bprogram;
  }

  public <T> T clone(T src) {
    try {
      T result = (T) deserialize(serialize(src));
      return result;
    } catch (IOException | ClassNotFoundException ex) {
      throw new RuntimeException("Failed to clone snapshot: " + ex.getMessage(), ex);
    }
  }

  private byte[] serialize(Object obj) throws IOException {
    try (Context cx = BPjs.enterRhinoContext()) {
      try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
           BPJSStubOutputStream outs = new BPJSStubOutputStream(bytes, bprogram.getGlobalScope())) {
        outs.writeObject(obj);
        outs.flush();
        return bytes.toByteArray();
      }
    }
  }

  private Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
    try (Context cx = BPjs.enterRhinoContext()) {
      try (BPJSStubInputStream in = new BPJSStubInputStream(new ByteArrayInputStream(bytes), bprogram.getGlobalScope(), getStubProvider())
      ) {
        return in.readObject();
      }
    }
  }

  private StubProvider getStubProvider() {
    final BProgramJsProxy bpProxy = new BProgramJsProxy(bprogram);
    if ( stubProvider == null ) {
      stubProvider = (StreamObjectStub stub) -> {
        if (stub == StreamObjectStub.BP_PROXY) {
          return bpProxy;
        }
        throw new IllegalArgumentException("Unknown stub " + stub);
      };
    }
    return stubProvider;
  }
}
