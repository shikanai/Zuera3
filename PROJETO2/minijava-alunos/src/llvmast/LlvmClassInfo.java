package llvmast;

public class LlvmClassInfo extends LlvmType {

	public String name;
	
	public LlvmClassInfo(String nome) {
		this.name = nome;
	}

	public String toString(){
		return "%class." + this.name;
	}
}
	
