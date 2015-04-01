package llvmast;
public class LlvmConstantDeclaration extends LlvmInstruction{
    public String name;
    public String rhs;
    public LlvmConstantDeclaration(String name, String rhs){
	this.name = name;
	this.rhs = rhs;
    }
    
    public String toString(){
	return name + " = " + rhs;
    }
}