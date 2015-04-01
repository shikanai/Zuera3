package llvmast;
import java.util.*;
public class LlvmFunctionType extends LlvmType{
    public LlvmType resultType;
    public List<LlvmType> parametersTypes;
    
    public LlvmFunctionType(LlvmType resultType, List<LlvmType> parametersTypes){
	this.resultType = resultType;
	this.parametersTypes = parametersTypes;
    }


}