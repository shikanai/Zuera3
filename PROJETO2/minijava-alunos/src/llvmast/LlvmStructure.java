package llvmast;
import java.util.*;

import syntaxtree.VarDecl;
public class LlvmStructure extends LlvmType{
    public int sizeByte;
    public List<LlvmType> typeList;
    
    public LlvmStructure(List<LlvmType> typeList){
    	this.typeList = typeList;
    	
    	// Fazendo a contagem do tamanho da estrutura, caso precise de Malloc
	for (LlvmType T : typeList){
		if ( T instanceof LlvmPointer ){ 
			sizeByte += 8;
		} else {
			if ( T instanceof LlvmPrimitiveType){
				if (T.toString().equals("i32")){
					sizeByte += 4;
				} else {
					sizeByte += 1;
				}	
			}
		}
	}
    }
    
    public String toString() {
    	if (typeList.isEmpty())
    		return "{ }";
    	
    	String S = "{ " + typeList.get(0);
		for (int i = 1; i < typeList.size(); i++){
			S += ", "+typeList.get(i).toString();
		}
    	S += " }";
    	return S;
    }
    
}
