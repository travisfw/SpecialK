package com.biosimilarity.lift.lib.json.Absyn; // Java Package generated by the BNF Converter.

public abstract class JSONArray implements java.io.Serializable {
  public abstract <R,A> R accept(JSONArray.Visitor<R,A> v, A arg);
  public interface Visitor <R,A> {
    public R visit(com.biosimilarity.lift.lib.json.Absyn.JArray p, A arg);

  }

}
