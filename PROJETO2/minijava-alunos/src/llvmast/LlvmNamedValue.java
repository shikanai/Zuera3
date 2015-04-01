package llvmast;
public class LlvmNamedValue extends LlvmRegister{

	public LlvmNamedValue(String name, LlvmType type){
		super(name, type);
		this.name = name;
	}
	
	public String toString(){
		return name; 
	}
}
