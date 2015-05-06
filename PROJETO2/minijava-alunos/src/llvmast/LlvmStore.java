package llvmast;
public class LlvmStore extends LlvmInstruction{
    public LlvmValue content; 
    public LlvmValue address; 

    public LlvmStore(LlvmValue content, LlvmValue address){
	this.content=content;
	this.address=address;
    }
    
    public String toString(){

    	//System.out.format("teste de migueh no store... detectando class: %s %s %s\n",this.address,this.address.type,this.address.type.toString());
    	
    	if(this.address.type.toString().contains("%class.")){
    		//System.out.format("detectado class!!\n");
    		return "  store " + content.type + " " + content + ", " + address.type + "* " + address;
    	}else{
    		//System.out.format("nao detectado class...\n");
    		
    		return "  store " + content.type + " " + content + ", " + address.type + " " + address;
    	}
    }
}
