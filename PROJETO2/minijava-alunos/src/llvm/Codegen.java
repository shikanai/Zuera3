/*****************************************************
Esta classe Codegen é a responsável por emitir LLVM-IR. 
Ela possui o mesmo método 'visit' sobrecarregado de
acordo com o tipo do parâmetro. Se o parâmentro for
do tipo 'While', o 'visit' emitirá código LLVM-IR que 
representa este comportamento. 
Alguns métodos 'visit' já estão prontos e, por isso,
a compilação do código abaixo já é possível.

class a{
    public static void main(String[] args){
    	System.out.println(1+2);
    }
}

O pacote 'llvmast' possui estruturas simples 
que auxiliam a geração de código em LLVM-IR. Quase todas 
as classes estão prontas; apenas as seguintes precisam ser 
implementadas: 

// llvmasm/LlvmBranch.java
// llvmasm/LlvmIcmp.java
// llvmasm/LlvmMinus.java
// llvmasm/LlvmTimes.java


Todas as assinaturas de métodos e construtores 
necessárias já estão lá. 


Observem todos os métodos e classes já implementados
e o manual do LLVM-IR (http://llvm.org/docs/LangRef.html) 
como guia no desenvolvimento deste projeto. 

****************************************************/
package llvm;

import semant.Env;
import syntaxtree.*;
import llvmast.*;

import java.util.*;

public class Codegen extends VisitorAdapter{
	private List<LlvmInstruction> assembler;
	private Codegen codeGenerator;

  	private SymTab symTab;
	private ClassNode classEnv; 	// Aponta para a classe atualmente em uso em symTab
	private MethodNode methodEnv; 	// Aponta para a metodo atualmente em uso em symTab

	public static int counter_label = 0;

	public Codegen(){
		assembler = new LinkedList<LlvmInstruction>();
	}

	// Método de entrada do Codegen
	public String translate(Program p, Env env){	
		codeGenerator = new Codegen();
		
		// Preenchendo a Tabela de Símbolos
		// Quem quiser usar 'env', apenas comente essa linha
		// codeGenerator.symTab.FillTabSymbol(p);
		
		// Formato da String para o System.out.printlnijava "%d\n"
		codeGenerator.assembler.add(new LlvmConstantDeclaration("@.formatting.string", "private constant [4 x i8] c\"%d\\0A\\00\""));	

		// NOTA: sempre que X.accept(Y), então Y.visit(X);
		// NOTA: Logo, o comando abaixo irá chamar codeGenerator.visit(Program), linha 75
		p.accept(codeGenerator);

		// Link do printf
		List<LlvmType> pts = new LinkedList<LlvmType>();
		pts.add(new LlvmPointer(LlvmPrimitiveType.I8));
		pts.add(LlvmPrimitiveType.DOTDOTDOT);
		codeGenerator.assembler.add(new LlvmExternalDeclaration("@printf", LlvmPrimitiveType.I32, pts)); 
		List<LlvmType> mallocpts = new LinkedList<LlvmType>();
		mallocpts.add(LlvmPrimitiveType.I32);
		codeGenerator.assembler.add(new LlvmExternalDeclaration("@malloc", new LlvmPointer(LlvmPrimitiveType.I8),mallocpts)); 


		String r = new String();
		for(LlvmInstruction instr : codeGenerator.assembler)
			r += instr+"\n";
		return r;
	}

	public LlvmValue visit(Program n){
		n.mainClass.accept(this);

		for (util.List<ClassDecl> c = n.classList; c != null; c = c.tail)
			c.head.accept(this);

		return null;
	}

	public LlvmValue visit(MainClass n){
		
		// definicao do main 
		assembler.add(new LlvmDefine("@main", LlvmPrimitiveType.I32, new LinkedList<LlvmValue>()));
		assembler.add(new LlvmLabel(new LlvmLabelValue("entry")));
		LlvmRegister R1 = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I32));
		assembler.add(new LlvmAlloca(R1, LlvmPrimitiveType.I32, new LinkedList<LlvmValue>()));
		assembler.add(new LlvmStore(new LlvmIntegerLiteral(0), R1));

		// Statement é uma classe abstrata
		// Portanto, o accept chamado é da classe que implementa Statement, por exemplo,  a classe "Print". 
		n.stm.accept(this);  

		// Final do Main
		LlvmRegister R2 = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmLoad(R2,R1));
		assembler.add(new LlvmRet(R2));
		assembler.add(new LlvmCloseDefinition());
		return null;
	}
	
	public LlvmValue visit(Plus n){
		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmPlus(lhs,LlvmPrimitiveType.I32,v1,v2));
		return lhs;
	}
	
	public LlvmValue visit(Print n){

		LlvmValue v =  n.exp.accept(this);

		// getelementptr:
		LlvmRegister lhs = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I8));
		LlvmRegister src = new LlvmNamedValue("@.formatting.string",new LlvmPointer(new LlvmArray(4,LlvmPrimitiveType.I8)));
		List<LlvmValue> offsets = new LinkedList<LlvmValue>();
		offsets.add(new LlvmIntegerLiteral(0));
		offsets.add(new LlvmIntegerLiteral(0));
		List<LlvmType> pts = new LinkedList<LlvmType>();
		pts.add(new LlvmPointer(LlvmPrimitiveType.I8));
		List<LlvmValue> args = new LinkedList<LlvmValue>();
		args.add(lhs);
		args.add(v);
		assembler.add(new LlvmGetElementPointer(lhs,src,offsets));

		pts = new LinkedList<LlvmType>();
		pts.add(new LlvmPointer(LlvmPrimitiveType.I8));
		pts.add(LlvmPrimitiveType.DOTDOTDOT);
		
		// printf:
		assembler.add(new LlvmCall(new LlvmRegister(LlvmPrimitiveType.I32),
				LlvmPrimitiveType.I32,
				pts,				 
				"@printf",
				args
				));
		return null;
	}
	
	public LlvmValue visit(IntegerLiteral n){
		return new LlvmIntegerLiteral(n.value);
	};
	
	// Todos os visit's que devem ser implementados	
	public LlvmValue visit(ClassDeclSimple n){
		
		//seguindo o padrao do slide 21/47 parte 1 llvm...
		
		System.out.format("classdeclsimple :)\n");
		
		int i, j;
		
		//criando uma string, onde serao concatenados os tipos das variaveis da classe.
		StringBuilder classTypes = new StringBuilder();
		
		//criando uma string, onde serao concatenados os tipos das variaveis da classe.
		StringBuilder classMethods = new StringBuilder();
		
		//criando uma string onde sera colocado o nome da classe
		StringBuilder className = new StringBuilder();
		
		//%class.name
		className.append("%class.");
		className.append(n.name.s);
		
		//type { type1, type2, ... }
		classTypes.append("type { ");				
		if (n.varList != null) {
			j = n.varList.size();
			System.out.format("Numero de variaveis: %d\n", j);
			System.out.format("tipos das variaveis:\n");
			for (i = 0; i < j; i++){
				//itera a lista de variaveis para pegar todos os tipos e appendar em classTypes.
				LlvmValue variable_type = n.varList.head.type.accept(this);
				
				System.out.format("%s ", variable_type);
				
				//appenda os tipos de variaveis
				classTypes.append(variable_type);
				
				if (i + 1 < j){
					classTypes.append(", ");
				}
				
				n.varList = n.varList.tail;
			}
		}
		classTypes.append(" }");
		
		System.out.format("\nclassType final: %s\n", classTypes);
		
		System.out.format("className: %s\n",className);
		
		// Adiciona declaracao de classe no assembly
		assembler.add(new LlvmConstantDeclaration(className.toString(),classTypes.toString()));

		//continuar parte de methods.
		
		return null;
		
	}
	public LlvmValue visit(ClassDeclExtends n){
		System.out.format("classdeclextends :)\n");
		return null;
		
	}
	public LlvmValue visit(VarDecl n){
		System.out.format("vardecl :)\n");
		return null;
		
	}
	public LlvmValue visit(MethodDecl n){
		System.out.format("methoddecl :)\n");
		return null;
		
	}
	public LlvmValue visit(Formal n){
		System.out.format("formal :)\n");
		return null;
		
	}
	public LlvmValue visit(IntArrayType n){
		System.out.format("intarraytype :)\n");
		
		//retorna novo tipo criado... Verificar se da certo haha :P
		return LlvmPrimitiveType.I32PTR;
		
	}
	//test
	//Como nao podemos retornar um LlvmType, extendemos LlvmType para LlvmValue
	public LlvmValue visit(BooleanType n){
		return LlvmPrimitiveType.I1;
	}
	//test
	//Como nao podemos retornar um LlvmType, extendemos LlvmType para LlvmValue
	public LlvmValue visit(IntegerType n){
		return LlvmPrimitiveType.I32;
	}
	public LlvmValue visit(IdentifierType n){
		System.out.format("identifiertype :)\n");
		return null;
		
	}
	public LlvmValue visit(Block n){
		System.out.format("block :)\n");
		return null;
		
	}
	public LlvmValue visit(If n){
		//pega o registrador referente a condicao do if em cond
		LlvmValue cond = n.condition.accept(this);
		System.out.format("cond: %s\n",cond);
		System.out.format("n.condition: %s\n", n.condition);
		
		//cria as labels referentes a cada branch
		LlvmLabelValue brTrue = new LlvmLabelValue("iflabeltrue"+counter_label);
		counter_label++;
		System.out.format("label1: %s\n",brTrue);
		LlvmLabelValue brFalse = new LlvmLabelValue("iflabelfalse"+counter_label);
		counter_label++;
		System.out.format("label2: %s\n",brFalse);
		//cria label referente ao break da label para a qual pulamos.
		LlvmLabelValue brBreak = new LlvmLabelValue("iflabelbreak"+counter_label);
		counter_label++;
		System.out.format("label3: %s\n",brBreak);
		
		if(n.elseClause!=null && n.thenClause!=null){
			//faz o branch condicional, pulando para label brTrue se a cond retornar 1, e para brFalse se retornar 0
			assembler.add(new LlvmBranch(cond, brTrue, brFalse));

			//label brTrue
			assembler.add(new LlvmLabel(brTrue));
			
			//gera codigo contido na thenClause
			n.thenClause.accept(this);

			System.out.format("thenClause: %s\n",n.thenClause);

			//se pulou para label brTrue, agora ele pula para o brBreak, com o intuito de pular o brFalse
			assembler.add(new LlvmBranch(brBreak));
			
			assembler.add(new LlvmLabel(brFalse));
			
			System.out.format("elseClause: %s\n",n.elseClause);

			//gera o codigo contido na elseClause
			n.elseClause.accept(this);
			
		} else if(n.elseClause!=null && n.thenClause==null){
			//faz o branch condicional, pulando para label brBreak se a cond retornar 1, e para brFalse se retornar 0
			assembler.add(new LlvmBranch(cond, brBreak, brFalse));

			System.out.format("elseClause != null");
		
			assembler.add(new LlvmLabel(brFalse));

			System.out.format("elseClause: %s\n",n.elseClause);

			//gera o codigo contido na elseClause
			n.elseClause.accept(this);
			
		} else {
			//faz o branch condicional, pulando para label brTrue se a cond retornar 1, e para brBreak se retornar 0
			assembler.add(new LlvmBranch(cond, brTrue, brBreak));
			//label brTrue
			assembler.add(new LlvmLabel(brTrue));
			
			//gera codigo contido na thenClause
			n.thenClause.accept(this);

			System.out.format("thenClause: %s\n",n.thenClause);
			
			//se pulou para label brTrue, agora ele pula para o brBreak, com o intuito de pular o brFalse
			assembler.add(new LlvmBranch(brBreak));
		}
		//assembler.add(new LlvmBranch(brBreak));
		//label do break
		assembler.add(new LlvmLabel(brBreak));
		
		return null;
	}
	public LlvmValue visit(While n){
		
		//pega o registrador referente a condicao do while em cond
		LlvmValue cond = n.condition.accept(this);
		System.out.format("cond: %s\n",cond);
		System.out.format("n.condition: %s\n", n.condition);
		
		//cria as labels referentes a cada branch
		
		//cria label referente ao while.
		LlvmLabelValue brTrue = new LlvmLabelValue("whilelabeltrue"+counter_label);
		counter_label++;
		System.out.format("label1: %s\n",brTrue);
		
		//cria label referente ao break do while.
		LlvmLabelValue brBreak = new LlvmLabelValue("whilelabelbreak"+counter_label);
		counter_label++;
		System.out.format("label2: %s\n",brBreak);
				
		//faz o branch condicional, pulando para label brTrue se a cond retornar 1, e para brBreak se retornar 0
		//(ou seja, da break quando nao atende mais a condicao)
		
		assembler.add(new LlvmBranch(cond, brTrue, brBreak));
				
		//label brTrue
		assembler.add(new LlvmLabel(brTrue));
		
		System.out.format("body: %s\n",n.body);
		
		//gera codigo contido dentro do while
		n.body.accept(this);
		
		//depois de executar o codigo dentro do while, faz de novo o branch. 
		assembler.add(new LlvmBranch(cond, brTrue, brBreak));
		
		//label do break
		assembler.add(new LlvmLabel(brBreak));
			
		return null;
	}
	public LlvmValue visit(Assign n){
		System.out.format("assign :)\n");
		return null;
		
	}
	public LlvmValue visit(ArrayAssign n){
		System.out.format("assign array :)\n");
		return null;
		
	}
	//test
	public LlvmValue visit(And n){
		
		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);

		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I1);
		LlvmRegister aux1 = new LlvmRegister(LlvmPrimitiveType.I1);
		
		//para termos certeza de que soh sera executado o codigo quando tivermos true e true, vamos usar o LlvmTimes
		//para multiplicar um pelo outro. Assim, quando ao menos um deles for false, teremos resposta zero
		
		//o tipo de v1 e v2 eh o mesmo, entao passamos o de v1
		assembler.add(new LlvmTimes(aux1, v1.type, v1, v2));
		
		LlvmIntegerLiteral aux2 = new LlvmIntegerLiteral(1);
		
		//compara o resultado da multiplicacao com 1(pois todas as vezes que um for false, o resultado de mul vai ser zero).
		assembler.add(new LlvmIcmp(lhs,1,aux1.type,aux1,aux2));
		
		//retorna o registrador contendo o resultado
		return lhs;
	}
	//test
	public LlvmValue visit(LessThan n){
		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I1);
		//utilizando 0 para lessthan
		assembler.add(new LlvmIcmp(lhs,0,LlvmPrimitiveType.I1,v1,v2));
		return lhs;
	}
	//ok
	public LlvmValue visit(Equal n){
		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I1);
		//utilizando 1 para equal
		assembler.add(new LlvmIcmp(lhs,1,LlvmPrimitiveType.I1,v1,v2));
		return lhs;
	}
	//ok
	public LlvmValue visit(Minus n){
		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmMinus(lhs,LlvmPrimitiveType.I32,v1,v2));
		return lhs;
	}
	//ok
	public LlvmValue visit(Times n){
		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmTimes(lhs,LlvmPrimitiveType.I32,v1,v2));
		return lhs;  
	}
	public LlvmValue visit(ArrayLookup n){
		System.out.format("arraylookup :)\n");
		return null;
		
	}
	public LlvmValue visit(ArrayLength n){
		System.out.format("arraylength :)\n");
		return null;
		
	}
	public LlvmValue visit(Call n){
		System.out.format("call :)\n");
		return null;
		
	}
	//ok
	public LlvmValue visit(True n){
		//1 -> true
		return new LlvmBool(1);
	}
	//ok
	public LlvmValue visit(False n){
		//0 -> false
		return new LlvmBool(0);
	}
	public LlvmValue visit(IdentifierExp n){
		System.out.format("identifierexp :)\n");
		return null;
		
	}
	//test
	public LlvmValue visit(This n){
		LlvmType thisType = (LlvmType) n.type.accept(this);
		LlvmRegister thisReg = new LlvmRegister("%this_register", thisType);
		return thisReg;
	}
	public LlvmValue visit(NewArray n){
		System.out.format("newarray :)\n");
		return null;
		
	}
	public LlvmValue visit(NewObject n){
		System.out.format("newobject :)\n");
		return null;
		
	}
	//ok
	public LlvmValue visit(Not n){
		//ideia era um xor com 1, porque ele inverte o bit. 1 xor 1 = 0 e 1 xor 0 = 1
		//Para nao implementar xor usou-se o Icmp com equal e 0 (primeiro termo fixo), 
		//pois 0 == 0 é true (1) e 0 == 1 é false, ou seja, 0.
		LlvmValue v1 = n.exp.accept(this);
        LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I1);
        assembler.add(new LlvmIcmp(lhs,1,v1.type,v1,new LlvmBool(0)));
        return lhs;
        
	}
	public LlvmValue visit(Identifier n){
		System.out.format("identifier :)\n");
		return null;
		
	}
}


/**********************************************************************************/
/* === Tabela de Símbolos ==== 
 * 
 * 
 */
/**********************************************************************************/

class SymTab extends VisitorAdapter{
    public Map<String, ClassNode> classes;     
    private ClassNode classEnv;    //aponta para a classe em uso

    public LlvmValue FillTabSymbol(Program n){
	n.accept(this);
	return null;
}
public LlvmValue visit(Program n){
	n.mainClass.accept(this);

	for (util.List<ClassDecl> c = n.classList; c != null; c = c.tail)
		c.head.accept(this);

	return null;
}

public LlvmValue visit(MainClass n){
	classes.put(n.className.s, new ClassNode(n.className.s, null, null));
	return null;
}

public LlvmValue visit(ClassDeclSimple n){
	List<LlvmType> typeList = null;
	// Constroi TypeList com os tipos das variáveis da Classe (vai formar a Struct da classe)
	
	List<LlvmValue> varList = null;
	// Constroi VarList com as Variáveis da Classe

	classes.put(n.name.s, new ClassNode(n.name.s, 
										new LlvmStructure(typeList), 
										varList)
      			);
    	// Percorre n.methodList visitando cada método
	return null;
}

	public LlvmValue visit(ClassDeclExtends n){return null;}
	public LlvmValue visit(VarDecl n){return null;}
	public LlvmValue visit(Formal n){return null;}
	public LlvmValue visit(MethodDecl n){return null;}
	public LlvmValue visit(IdentifierType n){return null;}
	public LlvmValue visit(IntArrayType n){return null;}
	public LlvmValue visit(BooleanType n){return null;}
	public LlvmValue visit(IntegerType n){return null;}
}

class ClassNode extends LlvmType {
	ClassNode (String nameClass, LlvmStructure classType, List<LlvmValue> varList){
	}
}

class MethodNode {
}




