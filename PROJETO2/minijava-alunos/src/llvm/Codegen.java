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
		symTab = new SymTab();
	}

	// Método de entrada do Codegen
	public String translate(Program p, Env env){	
		codeGenerator = new Codegen();
		
		// Preenchendo a Tabela de Símbolos
		// Quem quiser usar 'env', apenas comente essa linha
		codeGenerator.symTab.FillTabSymbol(p);
		
		System.out.format("***********terminando de preencher a SymTab...\n");
		
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

		//methodEnv = symTab.methods.get(n.name.s);
		//System.out.format("****methodEnv: \n%s \n%s \n%s \n%s\n",methodEnv.formals_name,methodEnv.formals_value,methodEnv.locals_name,methodEnv.locals_value);
		
		//recuperando classEnv do SymTab
		classEnv = symTab.classes.get(n.name.s);
		
		System.out.format("****classEnv: \n%s \n%s \n%s \n%s\n",classEnv.classType, classEnv.nameClass, classEnv.type, classEnv.varList);
		
		System.out.format("n: %s %s %s %s\n",n,n.name, n.methodList, n.varList);
		
		//seguindo o padrao do slide 21/47 parte 1 llvm...
		
		System.out.format("classdeclsimple*********************\n");
		
		int i, j;
		
		//criando uma string, onde sera colocado o assembly referente ao type. (type {...})
		StringBuilder classTypes = new StringBuilder();
		
		//criando uma string onde sera colocado o nome da classe
		StringBuilder className = new StringBuilder();
		
		//lista ligada onde serao armazenados os tipos das variaveis
		List<LlvmType> typeList = new LinkedList<LlvmType>();
		
		//%class.name
		className.append("%class.");
		className.append(n.name.s);
		
		//type { type1, type2, ... } - vamos utilizar LlvmStructure
		classTypes.append("type ");
		
		if (n.varList != null) {
			j = n.varList.size();
			System.out.format("Numero de variaveis: %d\n", j);
			for (i = 0; i < j; i++){
				
				//itera a lista de variaveis para pegar todos os tipos e appendar em classTypes.
				LlvmValue variable_type = n.varList.head.type.accept(this);
				
				System.out.format("tipos das variaveis:%s \n", variable_type);
				
				if(variable_type.toString().contains("%class")){
					System.out.format("eh uma classe. alterando para pointer...\n");
					LlvmPointer ptr_class = new LlvmPointer((LlvmType) variable_type);
					typeList.add(ptr_class);
					
				}else{
					//adiciona os tipos de variaveis
					typeList.add((LlvmType) variable_type);
				}
				n.varList = n.varList.tail;
			}
		}

		//Structure onde serao colocados os tipos, formatados pelo LlvmStructure
		LlvmStructure listaDeTipos = new LlvmStructure(typeList);
		
		//System.out.format("\nListaDetipos: %s\n", listaDeTipos.toString());
		
		if(listaDeTipos.toString().contains("null")){
			
			System.out.format("listaDeTipos nula\n");
			
			//appenda a lista de tipos no classTypes
			classTypes.append("{ }");
			
		}
		else{
		
			System.out.format("listaDeTipos nao nula\n");
			
			//appenda a lista de tipos no classTypes
			classTypes.append(listaDeTipos.toString());
			
		}
		System.out.format("\nclassType final: %s\n", classTypes);
		
		System.out.format("className: %s\n",className);
		
		// Adiciona declaracao de classe no assembly
		assembler.add(new LlvmConstantDeclaration(className.toString(),classTypes.toString()));

		System.out.format("antes methodenv: %s\n", n.methodList);
		
		//methodEnv = symTab.methods.get(n.methodList.head.name.s);
		
		//System.out.format("methodEnv: %s\n",methodEnv);
		
		if(n.methodList != null) {
			j = n.methodList.size();
			System.out.format("methodList.size: %s\n",n.methodList.size());
			//itera todos os metodos da classe
			for (i = 0; i < j; i++) {
				MethodDecl method = n.methodList.head;
				
				System.out.format("@class - method: %s ", method);
				
				//desce para methods
				
				method.accept(this);
				
				n.methodList = n.methodList.tail;
			}
		}
		
		return null;
		
	}
	
	//provavelmente nao vamos ter tempo de fazer esse...(espero que tenhamos :D)
	public LlvmValue visit(ClassDeclExtends n){
		System.out.format("classdeclextends*********************\n");
		return null;
		
	}
	
	//declaracao de variavel.
	public LlvmValue visit(VarDecl n){
		System.out.format("vardecl*********************\n");
		
		LlvmType varType = (LlvmType) n.type.accept(this);
		
		StringBuilder varDeclaration = new StringBuilder();
		
		//criando o nome do endereco onde sera alocada a variavel
		varDeclaration.append("%");
		varDeclaration.append(n.name.s);
		varDeclaration.append("_address");
		
		System.out.format("var addr name: %s\n",varDeclaration.toString());
		
		//varType *
		LlvmPointer varTypePtr = new LlvmPointer(varType);
		
		//%name_address
		LlvmRegister registerVar = new LlvmRegister(varDeclaration.toString(), varTypePtr);
		
		//gera o assembly: %name_address = alloca type
		assembler.add(new LlvmAlloca(registerVar, varType, new LinkedList<LlvmValue>()));
		
		return registerVar;
		
	}
	
	public LlvmValue visit(MethodDecl n){
		
		System.out.format("methoddecl*********************\n");
		//recuperando o methodEnv da symTab
		methodEnv = symTab.methods.get(n.name.s);
		System.out.format("****methodEnv: \n%s \n%s \n%s \n%s\n",methodEnv.formals_name,methodEnv.formals_value,methodEnv.locals_name,methodEnv.locals_value);
						
		int i,j;
		
		LinkedList<LlvmValue> parametros = new LinkedList<LlvmValue>();
		LlvmType retType = (LlvmType) n.returnType.accept(this);
		StringBuilder declString = new StringBuilder();
		
		
		
		if(n.body!=null){
			System.out.format("body.head: %s\n",n.body.head);
		}
		if(n.locals!=null){
			System.out.format("locals: %s\n",n.locals.head);
		}
		System.out.format("name: %s\n",n.name);
		System.out.format("return type: %s\n",n.returnType);
		System.out.format("return exp: %s\n",n.returnExp);
		
		//antes de sair preenchendo o formals, temos que colocar o %class * %this
		
		LlvmClassInfo class_this = new LlvmClassInfo(classEnv.nameClass);
		
		LlvmPointer class_this_ptr = new LlvmPointer(class_this);
		
		LlvmNamedValue this_val = new LlvmNamedValue("%this",class_this_ptr);
		
		System.out.format("this_val: \n%s \n%s \n%s\n",this_val, this_val.name, this_val.type);
		
		parametros.add(this_val);
		
		//preenchendo a lista de parametros do metodo
		if(n.formals != null) {
			System.out.format("formals: %s\n",n.formals.head);
			j = n.formals.size();
			System.out.format("formals size: %d\n", j);
			
			//itera todos os parametros do metodo - Como nao passamos novamente pelo codigo,
			//tudo bem deixar assim :P
			for (i = 0; i < j; i++) {
				
				LlvmValue param = n.formals.head.accept(this);
				
				System.out.format("formals: %s \n", n.formals.head);
				parametros.add(param);
				n.formals = n.formals.tail;
				
			}
		}
		
		declString.append("@__");
		declString.append(n.name);
		
		System.out.format("declString: %s\n",declString);
		System.out.format("retType: %s\n",retType);
		
		//se for retornar uma classe, retorna ponteiro para tipo da classe.
		if(retType.toString().contains("%class")){
			LlvmPointer ptr_retType = new LlvmPointer(retType);
			// Adiciona define de classe no assembly
			assembler.add(new LlvmDefine(declString.toString(), ptr_retType, parametros));
		}else{
			// Adiciona define de classe no assembly
			assembler.add(new LlvmDefine(declString.toString(), retType, parametros));
		}
		//Apos o define, devemos comecar a implementacao do metodo...
		
		//copiando da main... Criando entrypoint
		assembler.add(new LlvmLabel(new LlvmLabelValue("entryMethod")));
		
		j = parametros.size();
		
		//alocando memoria para todos os parametros, menos o referente a classe
		for(i = 1; i < j ; i++){
			
			LlvmValue parametro_atual = parametros.get(i);
			
			System.out.format("parametro_atual: %s\n",parametro_atual);
			
			//cria ponteiro para o tipo, que sera utilizado como endereco de alocacao.
			LlvmPointer pointer_type = new LlvmPointer(parametro_atual.type);
			
			StringBuilder addr_name = new StringBuilder();
			
			addr_name.append(parametro_atual);
			addr_name.append("_address");
			
			//%name_address
			LlvmValue addr = new LlvmNamedValue(addr_name.toString(), pointer_type);
			
			//aloca memoria para endereco
			assembler.add(new LlvmAlloca(addr, parametro_atual.type, new LinkedList<LlvmValue>()));
			
			//armazena valor no endereco apontado por addr
			assembler.add(new LlvmStore(parametro_atual,addr));
		}
		
		
		
		//itera todas as variaveis locais
		
		if(n.locals!=null){
			System.out.format("locals size: %s\n",n.locals.size());
			j = n.locals.size();
			
			//Como nao passamos novamente pelo codigo,
			//tudo bem deixar assim :P
			for(i=0;i<j;i++){
				
				//chama varDecl para cada variavel local
				n.locals.head.accept(this);
				
				System.out.format("locals****: %s \n", n.locals.head);
				
				n.locals = n.locals.tail;
				
			}
		}
		
		//itera nos statements do metodo
		
		if(n.body!=null){
			
			j = n.body.size();
			
			//Como nao passamos novamente pelo codigo,
			//tudo bem deixar assim :P
			for(i=0;i<j;i++){
					
				System.out.format("body****: %s \n", n.body.head);
				
				//desce para stmt seguinte.
				n.body.head.accept(this);
						
				n.body = n.body.tail;
						
			}
		}

		//retorno...
		assembler.add(new LlvmRet(n.returnExp.accept(this)));
		
		//}
		assembler.add(new LlvmCloseDefinition());
		
		return null;
		
	}
	
	//variavel de parametro...
	public LlvmValue visit(Formal n){
		System.out.format("formal :)\n");
		
		StringBuilder name = new StringBuilder();
		
		name.append("%");
		name.append(n.name.s);
		
		//variable -> %name, com tipo n.type
		LlvmNamedValue variable = new LlvmNamedValue(name.toString(), (LlvmType)n.type.accept(this));
		
		return variable;		
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
	
	//de forma alguma consegui pensar em como fazer isso sem adicionar uma nova classe
	//entao criei o LlvmClassInfo
	
	public LlvmValue visit(IdentifierType n){
		System.out.format("identifiertype :)\n");
		
		//%class.name
		StringBuilder name = new StringBuilder();
		
		//name.append("%class.");
		name.append(n.name);
		
		System.out.format("name: %s\n",name.toString());
		
		//cria classType
		LlvmClassInfo classType = new LlvmClassInfo(name.toString());
		
		//Armazena o identifiertype na variavel global (relativa a classe)
		//desnecessario, agora que temos o symtab
		//ClassInfo = new LlvmClassInfo(name.toString());
		//System.out.format("***ClassInfo: %s\n",ClassInfo.toString());
		
		//%class.name *
		//LlvmPointer classTypePtr = new LlvmPointer(classType);
		
		//return classTypePtr;		

		return classType;
		
	}
	
	//na implementacao do block ,simplesmente iteramos o body inteiro do block
	public LlvmValue visit(Block n){
		System.out.format("block :)\n");
		
		//verifica se existe algum elemento no block
		if(n.body != null){
			int i,j;
			
			j = n.body.size();
			
			//itera em todos os elementos - como ja passamos pelo symtab,
			//acho que eh ok deixar desse jeito... senao, mudar dps.
			for(i=0;i<j;i++){
			
				System.out.format("@block body: %s\n", n.body.head);
				
				//desce para cada parte do block
				n.body.head.accept(this);
						
				n.body = n.body.tail;
				
				
			}
		}
		
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
			
			//desce para thenClause
			n.thenClause.accept(this);

			System.out.format("thenClause: %s\n",n.thenClause);

			//se pulou para label brTrue, agora ele pula para o brBreak, com o intuito de pular o brFalse
			assembler.add(new LlvmBranch(brBreak));
			
			assembler.add(new LlvmLabel(brFalse));
			
			System.out.format("elseClause: %s\n",n.elseClause);

			//desce para elseClause
			n.elseClause.accept(this);
			
			assembler.add(new LlvmBranch(brBreak));
			
		} else if(n.elseClause!=null && n.thenClause==null){
			//faz o branch condicional, pulando para label brBreak se a cond retornar 1, e para brFalse se retornar 0
			assembler.add(new LlvmBranch(cond, brBreak, brFalse));

			System.out.format("elseClause != null");
		
			assembler.add(new LlvmLabel(brFalse));

			System.out.format("elseClause: %s\n",n.elseClause);

			//desce para elseClause
			n.elseClause.accept(this);
			
			assembler.add(new LlvmBranch(brBreak));
		} else {
			//faz o branch condicional, pulando para label brTrue se a cond retornar 1, e para brBreak se retornar 0
			assembler.add(new LlvmBranch(cond, brTrue, brBreak));
			//label brTrue
			assembler.add(new LlvmLabel(brTrue));
			
			//desce para thenClause
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
		
		//desce para body do while
		n.body.accept(this);
		
		//depois de executar o codigo dentro do while, faz de novo o branch. 
		assembler.add(new LlvmBranch(cond, brTrue, brBreak));
		
		//label do break
		assembler.add(new LlvmLabel(brBreak));
			
		return null;
	}
	
	//nesta chamada, devemos pegar o address da variavel e dar um store no valor do assign nesse endereco
	public LlvmValue visit(Assign n){
		
		System.out.format("assign :)\n");
		/*
		//valor que vai ser armazenado no endereco apontado pela variavel
		LlvmValue value_to_store = n.exp.accept(this);
				
		System.out.format("n.exp: %s\n",n.exp);
				
		LlvmValue var = n.var.accept(this);
		StringBuilder address = new StringBuilder();
		
		System.out.format("var_name: %s\n",var);
		//System.out.format("n.var: %s\n",n.var);
		System.out.format("n string: %s\n", n.toString());
		
		address.append("%");
		address.append(var.toString());
		address.append("_address");
		
		LlvmType var_type = var.type;
		
		System.out.format("var_type: %s\n",var.type);
		
		//%name_address
		LlvmValue pointer = new LlvmNamedValue(address.toString(), new LlvmPointer(var_type));
		
		//gera assembly referente ao store: store type %reg, type * %address;
		assembler.add(new LlvmStore(value_to_store, pointer));
		
		//return value_to_store;
		return null;
		*/
		
		LlvmValue rhs = n.exp.accept(this);
		LlvmRegister returns;
		if(rhs.type.toString().contains("x i")){
			System.out.format("expressao de rhs envolve pointers para arrays. fazendo casting...\n");
			
			//fazer bitcast
			if(rhs.type.toString().contains(" x i32")){
				returns = new LlvmRegister(LlvmPrimitiveType.I32PTR);
			}else if(rhs.type.toString().contains(" x i8")){
				returns = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I8));
			}else{
				returns = new LlvmRegister(rhs.type);
			}
			
			assembler.add(new LlvmBitcast(returns, rhs, returns.type));
			assembler.add(new LlvmStore(returns, n.var.accept(this)));
		}else{
			assembler.add(new LlvmStore(rhs, n.var.accept(this)));
		}
		return null;
	}
	
	//ToDo
	public LlvmValue visit(ArrayAssign n){
		System.out.format("assign array :)\n");
		
		//A ideia eh um lookup dando store na posicao que eu pegar
		//Teste nao deu certo..
		LlvmValue array = n.var.accept(this);
		LlvmValue index = n.index.accept(this);
		LlvmValue value = n.value.accept(this);
		
		System.out.format("array assign var,index,value: %s\n%s\n%s\n",array,index,value);
		
		LlvmRegister reg = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I32));
		List<LlvmValue> offsets = new LinkedList<LlvmValue>();
		offsets.add(index);
		
		//carregando a array, agora meu reg_array_ptr vai ter a array em questao
		LlvmRegister reg_array_ptr = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I32));
		assembler.add(new LlvmLoad(reg_array_ptr, array));
		
		//peguei ponteiro para o ponteiro do elemento que queremos.
		//Agora preciso carregar esse ponteiro em outro registrador, para utilizar ele.
		assembler.add(new LlvmGetElementPointer(reg, reg_array_ptr, offsets));
		
		//LlvmRegister reg_address_offset = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I32));
		
		//assembler.add(new LlvmLoad(reg_address_offset, reg));
		
		//System.out.format("regtype arraytype: %s %s :)\n",reg.type, array.type);
		
		//assembler.add(new LlvmLoad());
		assembler.add(new LlvmStore(value, reg));
		
		return null;
	}

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

	public LlvmValue visit(LessThan n){
		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I1);
		//utilizando 0 para lessthan
		
		System.out.format("less than: lhs e rhs: %s %s\n",n.lhs,n.rhs);
		
		//comparamos dois elementos i32, e retornamos um bool.
		assembler.add(new LlvmIcmp(lhs,0,v1.type,v1,v2));
		return lhs;
	}
	//ok
	public LlvmValue visit(Equal n){
		LlvmValue v1 = n.lhs.accept(this);
		LlvmValue v2 = n.rhs.accept(this);
		LlvmRegister lhs = new LlvmRegister(LlvmPrimitiveType.I1);
		//utilizando 1 para equal
		assembler.add(new LlvmIcmp(lhs,1,v1.type,v1,v2));
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
		
		LlvmValue array = n.array.accept(this);
		LlvmValue index = n.index.accept(this);
		
		System.out.format("arraylookup array e index: %s \n%s\n",array,index);
		
		LlvmRegister reg = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I32));
		List<LlvmValue> offsets = new LinkedList<LlvmValue>();
		offsets.add(index);
		/***************************************************/
		//carregando a array, agora meu reg_array_ptr vai ter a array em questao
		LlvmRegister reg_array_ptr = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I32));
		assembler.add(new LlvmLoad(reg_array_ptr, array));
				
		//peguei ponteiro para o ponteiro do elemento que queremos.
		//Agora preciso carregar esse ponteiro em outro registrador, para utilizar ele.
		assembler.add(new LlvmGetElementPointer(reg, reg_array_ptr, offsets));
				
		//LlvmRegister reg_address_offset = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I32));
				
		//assembler.add(new LlvmLoad(reg_address_offset, reg));
				
		//System.out.format("regtype arraytype: %s %s :)\n",reg.type, array.type);
				
		//assembler.add(new LlvmLoad());
		//assembler.add(new LlvmStore(value, reg));
		/***********************************************/
		
		LlvmRegister reg_address_offset = new LlvmRegister(LlvmPrimitiveType.I32);
		
		assembler.add(new LlvmLoad(reg_address_offset, reg));
		
		return reg_address_offset;
		
		/*//peguei ponteiro para o ponteiro do elemento que queremos.
		//Agora preciso carregar esse ponteiro em outro registrador, para utilizar ele.
		assembler.add(new LlvmGetElementPointer(reg, array, offsets));
		
		LlvmRegister reg_address_offset = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I32));
		
		assembler.add(new LlvmLoad(reg_address_offset, reg));
		
		LlvmRegister value_to_return = new LlvmRegister(LlvmPrimitiveType.I32);
		assembler.add(new LlvmLoad(value_to_return, reg_address_offset));
		
		//System.out.format("regtype arraytype: %s %s :)\n",reg.type, array.type);
		
		return value_to_return;*/
		
		
	}
	//nao consegui acessar o length da array.
	//Portanto, vamos ter que carregar a array e verificar
	public LlvmValue visit(ArrayLength n){
		
		int index = 0;
		char type_char;
		System.out.format("arraylength :)\n");

		//System.out.format("arraylength %d:)\n", Integer.parseInt(tamanho.toString()));
		System.out.format("****n, n.array: %s \n%s\n",n,n.array);
		
		//desce para o array, e pega o registrador que aponta para ela em array
		LlvmValue array = n.array.accept(this);
		
		System.out.format("array e array.type: %s \n%s\n",array,array.type);
		
		StringBuilder type = new StringBuilder();
		type.append(array.type.toString());
		
		System.out.format("type: %s\n",type);
		
		StringBuilder lengths = new StringBuilder();
		
		
		//agora vamos parsear esse tamanho, em busca do tamanho total da string...
		
		while(true){
			type_char = type.charAt(index);
			if(type_char=='i'){
				System.out.format("fim\n");
				break;
			}
			System.out.format("char atual:%c\n",type.charAt(index));
			if(type_char=='x'){
				System.out.format("x\n");
				lengths.append(" ");
			}
			//achei um numero...
			if(type_char != 'x' && type_char != ' ' && type_char != '['){
				System.out.format("numero: %c\n", type_char);
				lengths.append(type_char);
			}
			if(type_char == '['){
				System.out.format("[\n");
			}
			if(type_char == ' '){
				System.out.format("space\n");
			}
			index++;
		}
		
		System.out.format("lengths: %s\n", lengths);
		System.out.format("lengths.length: %s\n", lengths.length());
		index = 0;
		
		StringBuilder length = new StringBuilder();
		int total_length = 1;
		while(index < lengths.length()){
			type_char = lengths.charAt(index);
			if(type_char == '\0'){
				System.out.format("fim\n");
				break;
			}
			if(type_char!=' '){
				length.append(type_char);
			}else{
				System.out.format("space\n");
				total_length = total_length * Integer.parseInt(length.toString());
				System.out.format("total_length: %d\n",total_length);
				length = new StringBuilder();
			}
			index++;
		}
		
		LlvmIntegerLiteral length_final = new LlvmIntegerLiteral(total_length);
		
		return length_final;
		
	}
	public LlvmValue visit(Call n){
		
		System.out.format("call :)\n");
		
		System.out.format("call: %s\n",n);

		LlvmType type = (LlvmType) n.type.accept(this);
		
		LlvmRegister register = new LlvmRegister(type);
		
		System.out.format("type:%s\n", n.type);
		
		//lista dos tipos dos parametros da funcao sendo chamada
		List<LlvmType> types_list = new ArrayList<LlvmType>();
		
		//lista dos parametros da funcao sendo chamada
		List<LlvmValue> parametros_func = new ArrayList<LlvmValue>();
		
		int i, j; 
		
		//primeiro adicionamos o this object
		LlvmValue thisObject = n.object.accept(this);
		
		types_list.add(thisObject.type);
		parametros_func.add(thisObject);
		
		//depois os parametros de vdd
		if(n.actuals != null){
			
			System.out.format("size actuals:%s\n", n.actuals.size());
			
			j = n.actuals.size();
			
			for (util.List<Exp> actuals = n.actuals; actuals != null; actuals = actuals.tail){
				
				LlvmValue aux = actuals.head.accept(this);
				
				//tipo do parametro atual
				LlvmType aux2 =(LlvmType) aux.type;
				
				System.out.format("actuals:%s\n", n.actuals.head);
				
				System.out.format("actuals type:%s\n", n.actuals.head.type);
				
				//incrementando as duas listas
				types_list.add(aux2);
				parametros_func.add(aux);
			}
			
			/*
			//iterando todos os parametros - tinha errado nessa parte!
			for(i = 0; i < j; i++){
				
				//parametro atual
				LlvmValue aux = n.actuals.head.accept(this);
				
				//tipo do parametro atual
				LlvmType aux2 =(LlvmType) aux.type;
				
				System.out.format("actuals:%s\n", n.actuals.head);
				
				System.out.format("actuals type:%s\n", n.actuals.head.type);
				
				//incrementando as duas listas
				types_list.add(aux2);
				parametros_func.add(aux);
				n.actuals = n.actuals.tail;
			}*/
		
		}
		
		System.out.format("@call n.method.s:%s\n", n.method.s);
		
		//gerando o assembly do call da funcao, alterando o nome dela para "@__"+nome, pois esse eh o padrao dos metodos que estamos utilizando.
		assembler.add(new LlvmCall(register,type,types_list,"@__"+n.method.s,parametros_func));
		
		return register;
		
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
		
		System.out.format("n.type: %s\n",n.type);
		System.out.format("n.name: %s\n",n.name);
		System.out.format("n.string: %s\n",n.toString());
		
		//pega tipo do identificador
		LlvmPrimitiveType identifier_type = (LlvmPrimitiveType) n.type.accept(this);
		
		//registrador que vai ser retornado
		LlvmRegister returns = new LlvmRegister(identifier_type);
		
		//apontador vindo do identifier, que armazena a informacao que queremos.
		LlvmValue address = n.name.accept(this);
		System.out.format("@identifierexp address: %s\n",address);
		
		//ponteiro que passaremos para o load
		LlvmNamedValue needed_info_ptr = new LlvmNamedValue(address.toString(),address.type);
		
		System.out.format("@identifierexp address infos: %s %s\n", address.toString(), address.type);
		
		if(address.type.toString().contains("* *")){
			System.out.format("retornando ponteiro....\n");
			return needed_info_ptr;
		}
		
		//res = load type * %where_from_load
		assembler.add(new LlvmLoad(returns, needed_info_ptr));
		
		return returns;
		
	}
	//test
	public LlvmValue visit(This n){
		
		System.out.format("This******** :)\n");
		
		/*LlvmPointer classeAtual = null;
		
		LlvmPointer retorno = null;
		
		//ToDo adaptar
		//todo: adaptar para symtab
		why the hell decidi alocar algo para this!!!!
		if(classEnv!=null){
			
			LlvmClassInfo classInfo = new LlvmClassInfo(classEnv.nameClass);
			//%class *
			classeAtual = new LlvmPointer(classInfo);
			
			LlvmRegister regClasseAtual = new LlvmRegister("%this",classeAtual);
			//%class **
			LlvmPointer ptr_classeAtual = new LlvmPointer(classeAtual);
			
			LlvmRegister classe_aloc = new LlvmRegister("%this_address",ptr_classeAtual);
			
			assembler.add(new LlvmAlloca(classe_aloc,classeAtual, new LinkedList<LlvmValue>()));
			
			//assembler.add(new LlvmLoad(regClasseAtual, this_var));
			assembler.add(new LlvmLoad(regClasseAtual, classe_aloc));
			
			return regClasseAtual;
			
		}else{
			return null;
		}
		*/
		
		//Pega o tipo do object
		LlvmType thisType = (LlvmType) n.type.accept(this);
		
		LlvmPointer thisType_ptr = new LlvmPointer(thisType);
		
		System.out.format("thistype: %s\n",thisType_ptr);
		
		//cria um registrador referente ao this
		LlvmRegister thisReg = new LlvmRegister("%this",thisType_ptr);
		
		return thisReg;
		
	}
	public LlvmValue visit(NewArray n){
	
		System.out.format("newarray :)\n");
		
		LlvmValue tamanho = n.size.accept(this);
		LlvmType type = (LlvmType) n.type.accept(this);
		
		System.out.format("tamanho: %s :)\n",tamanho);
		
		if(tamanho.toString().contains("%")){
			
			return null;
		}else{
		
		//Como nos testes nao utilizamos array de outros tipos, fica assim por hora...
		LlvmType tipo_array = tamanho.type;
		
		System.out.format("tamanho.type: %s :)\n",tipo_array);
		int tamanho_int = Integer.parseInt(tamanho.toString());

		System.out.format("***Tamanho, tamanho_int, tipo, etc: %s %s %d %s %s:)\n",tamanho,tipo_array,tamanho_int,n.type,n.toString());
		
		// [10 x i32] *
		LlvmPointer tipo_ptr = new LlvmPointer(new LlvmArray(tamanho_int, tipo_array));
		
		//reg para [10 x i32] *
		LlvmRegister registrador = new LlvmRegister(tipo_ptr);
		
		//aloca uma array [tamanho x type], associa ao um registrador do tipo [tamanho x type]*
		assembler.add(new LlvmAlloca(registrador, new LlvmArray(tamanho_int, tipo_array), new LinkedList<LlvmValue>()));
		
		/*LlvmRegister returns;
		
		//fazer bitcast
		if(tipo_ptr.toString().contains(" x i32")){
			returns = new LlvmRegister(LlvmPrimitiveType.I32PTR);
		}else if(tipo_ptr.toString().contains(" x i8")){
			returns = new LlvmRegister(new LlvmPointer(LlvmPrimitiveType.I8));
		}else{
			returns = new LlvmRegister(tipo_ptr);
		}
		
		assembler.add(new LlvmBitcast(returns, registrador, returns.type));
		return returns;*/
		return registrador;
		}
		
	}
	public LlvmValue visit(NewObject n){
		System.out.format("newobject :)\n");
		
		System.out.format("n.type:%s :)\n",n.type);
		System.out.format("*****n:%s %s\n:)\n",n, n.className);
		StringBuilder address_str = new StringBuilder();
		
		//LlvmType type = ((LlvmPointer)n.type.accept(this)).content;
		LlvmType type = (LlvmType) (n.type.accept(this));
		
		System.out.format("n.type | n.type.content: %s %s :)\n",n.type,type);
		
		LlvmRegister res = new LlvmRegister(new LlvmPointer(type));
		
		address_str.append("%");
		
		address_str.append(n.className.toString());
		
		address_str.append("_address");
		
		//%class *
		LlvmRegister address = new LlvmRegister(address_str.toString(),new LlvmPointer(type));
		
		//gera o assembly: %name_address = alloca type
		assembler.add(new LlvmAlloca(address,new LlvmPointer(type), new LinkedList<LlvmValue>()));
		
		assembler.add(new LlvmMalloc(res, type, type.toString()));
		
		return res;
		
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

		System.out.format("********identifier: %s |\n",n);
		
		//TODO: descobrir como pegar o tipo do identifier
		//descoberto! usando symtab
		
		LlvmNamedValue identifier = null;
		int index;
		int i;
		LlvmValue identified;
		boolean aux;
		
		//para encontrar o tipo do identifier, vamos buscar na symtab, e ver o que ele realmente eh.
		
		System.out.format("bp\n");
		
		if(methodEnv != null){
			aux = methodEnv.formals_name.contains(n.s);
			System.out.format("formals: %s\n",aux);
			if(aux){
				index = methodEnv.formals_name.indexOf(n.s);
				identified = methodEnv.formals_value.get(index);
				System.out.format("identified: %s\n",identified);
				//encontrei valor nos formals. então a variavel eh um formals.
				identifier = new LlvmNamedValue("%"+n.s+"_address",new LlvmPointer(identified.type));
			}else{
				//se nao achou, vamos procurar no locals
				aux = methodEnv.locals_name.contains(n.s);
				System.out.format("locals: %s\n",aux);
				if(aux){
					index = methodEnv.locals_name.indexOf(n.s);
					identified = methodEnv.locals_value.get(index);
					System.out.format("identified: %s\n",identified);
					//encontrei valor nos locals. então a variavel eh um formals.
					identifier = new LlvmNamedValue("%"+n.s+"_address",new LlvmPointer(identified.type));
				}else{
					//se nao achou no locals, vamos procurar no classenv
					//dai devemos retornar um getelementptr
					//ToDo: suportar getelementptr
					
					StringBuilder var_name = new StringBuilder();
					
					var_name.append("%");
					var_name.append(n.s);
					var_name.append("_address");
					
					index = classEnv.varList.size();
					for(i=0;i<index;i++){
						LlvmValue var_atual = classEnv.varList.get(i);
						System.out.format("varList: %s\n",var_atual);
						if(var_atual.toString().contains(var_name.toString())){
							System.out.format("le migueh! encontrei o endereco da variavel.\n");
							LlvmType vartype = var_atual.type;
							System.out.format("@identifier: vartype: %s\n",vartype);
							//agora que encontramos o identifier, vamos usar o getelementptr para pegar os valores dele.
							
							//registrador no qual sera colocado o ponteiro para o elemento em questao. 
							//utilizamos LlvmPointer porque esse regisrtador vai apontar para o endereco que aponta para o vartype
							LlvmRegister returns = new LlvmRegister(new LlvmPointer(vartype));
							
							//variavel onde colocaremos os offsets
							List<LlvmValue> offsets = new LinkedList<LlvmValue>();
							
							//peguei da mainclass new LlvmIntegerLiteral(0)
							//primeiro indice: ponteiro para vetor de ponteiros (queremos sempre a base!)
							LlvmValue offset_1 = new LlvmIntegerLiteral(0);
							
							//segundo indice: indice do elemento do ponteiro apontado pelo indice 1.
							//como acabamos de dar match nele, colocamos i.
							LlvmValue offset_2 = new LlvmIntegerLiteral(i);
							
							//primeiro indice
							offsets.add(offset_1);
							
							//segundo indice
							offsets.add(offset_2);
							
							//do slide 21/47 da parte 1: %tmp0 = getelementptr %class.Matematica * %this, i32 0, i32 0
							
							//LlvmNamedValue classUsed = new LlvmNamedValue("%class."+classEnv.nameClass, vartype);
							
							LlvmClassInfo classType = new LlvmClassInfo(classEnv.nameClass);
							
							LlvmPointer classType_ptr = new LlvmPointer(classType);
							
							//Criando source (do GetElementPtr)
							LlvmNamedValue source = new LlvmNamedValue("%this",classType_ptr);
							
							assembler.add(new LlvmGetElementPointer(returns, source, offsets));
							
							System.out.format("returns:%s \n %s\n %s\n",returns,returns.name,returns.type);
							
							return returns;
							
						}
					}
					/*
					aux = classEnv.varList.contains(var_name);
					System.out.format("varlist: %s\n",aux);
					System.out.format("varname: %s\n",var_name);
					if(aux){
						identified = classEnv.varList.get(index);
						System.out.format("identified: %s\n",identified);
						identifier = new LlvmNamedValue("%"+n.s,identified.type);
					}else{
						System.out.format("deu ruim... nao eh nenhuma variavel...\n");
						identifier = new LlvmNamedValue("%"+n.s, new LlvmPointer(LlvmPrimitiveType.I32));
					}*/
				}
			}
		}else{
			//se nao achou no locals, vamos procurar no classenv
			//dai devemos retornar um getelementptr
			//ToDo: suportar getelementptr
			
			StringBuilder var_name = new StringBuilder();
			
			var_name.append("%");
			var_name.append(n.s);
			var_name.append("_address");
			
			index = classEnv.varList.size();
			for(i=0;i<index;i++){
				LlvmValue var_atual = classEnv.varList.get(i);
				System.out.format("varList: %s\n",var_atual);
				if(var_atual.toString().contains(var_name.toString())){
					System.out.format("le migueh! encontrei o endereco da variavel22.\n");
					LlvmType vartype = var_atual.type;
					System.out.format("@identifier: vartype: %s\n",vartype);
					//agora que encontramos o identifier, vamos usar o getelementptr para pegar os valores dele.
					
					//registrador no qual sera colocado o ponteiro para o elemento em questao. 
					//utilizamos LlvmPointer porque esse regisrtador vai apontar para o endereco que aponta para o vartype
					LlvmRegister returns = new LlvmRegister(new LlvmPointer(vartype));
					
					//variavel onde colocaremos os offsets
					List<LlvmValue> offsets = new LinkedList<LlvmValue>();
					
					//peguei da mainclass new LlvmIntegerLiteral(0)
					//primeiro indice: ponteiro para vetor de ponteiros (queremos sempre a base!)
					LlvmValue offset_1 = new LlvmIntegerLiteral(0);
					
					//segundo indice: indice do elemento do ponteiro apontado pelo indice 1.
					//como acabamos de dar match nele, colocamos i.
					LlvmValue offset_2 = new LlvmIntegerLiteral(i);
					
					//primeiro indice
					offsets.add(offset_1);
					
					//segundo indice
					offsets.add(offset_2);
					
					//do slide 21/47 da parte 1: %tmp0 = getelementptr %class.Matematica * %this, i32 0, i32 0
					
					//LlvmNamedValue classUsed = new LlvmNamedValue("%class."+classEnv.nameClass, vartype);
					
					LlvmClassInfo classType = new LlvmClassInfo(classEnv.nameClass);
					
					LlvmPointer classType_ptr = new LlvmPointer(classType);
					
					//Criando source (do GetElementPtr)
					LlvmNamedValue source = new LlvmNamedValue("%this",classType_ptr);
					
					assembler.add(new LlvmGetElementPointer(returns, source, offsets));
					
					System.out.format("returns:%s \n %s\n %s\n",returns,returns.name,returns.type);
					
					return returns;
				}
			}
			
			/*//se nao achou no locals, vamos procurar no classenv
			aux = classEnv.varList.contains(n.s);
			System.out.format("varlist2: %s\n",aux);
			if(aux){
				index = classEnv.varList.indexOf(n.s);
				identified = classEnv.varList.get(index);
				System.out.format("identified: %s\n",identified);
				identifier = new LlvmNamedValue("%"+n.s,identified.type);
			}else{
				System.out.format("deu ruim... nao eh nenhuma variavel...\n");
				identifier = new LlvmNamedValue("%"+n.s, LlvmPrimitiveType.I32);
			}*/
		}
		
		/*migueh antigo
		if(ClassInfo == null){
			System.out.format("********Armazenando I32\n");
			identifier = new LlvmNamedValue(n.s, new LlvmPointer(LlvmPrimitiveType.I32));
			identifier = new LlvmNamedValue(n.s, LlvmPrimitiveType.I32);
		}else{
			System.out.format("********Armazenando ClassInfo %s\n",ClassInfo.toString());
			identifier = new LlvmNamedValue(n.s, ClassInfo);
		}*/
		
		return identifier;
		
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
    public Map<String, MethodNode> methods;
    //sera utilizado nas visitas
    private ClassNode classEnv;    //aponta para a classe em uso
    private MethodNode methodEnv;   //aponta para metodo em uso
    //cnstrct
    public SymTab(){
    	classes = new HashMap<String, ClassNode>();
    	methods = new HashMap<String, MethodNode>();
    }
    
    public LlvmValue FillTabSymbol(Program n){
    	System.out.format("Comecando o preenchimento da symtab...\n");
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
	System.out.format("n.className: %s\n",n.className.s);
	
	System.out.format("Comecando o preenchimento da symtab MainClass...\n");
	classes.put(n.className.s, new ClassNode(n.className.s, null, null));
	System.out.format("Comecando o preenchimento da symtab MainClass22...\n");
	return null;
}

public LlvmValue visit(ClassDeclSimple n){
	
	System.out.format("@SymTabClassDeclSimple...\n");
	
	List<LlvmType> typeList = new LinkedList<LlvmType>();
	// Constroi TypeList com os tipos das variáveis da Classe (vai formar a Struct da classe)
	
	List<LlvmValue> varList = new LinkedList<LlvmValue>();
	// Constroi VarList com as Variáveis da Classe

	int i,j;
	
	if(n.varList!=null){
		//int varListSize = n.varList.size();
		
		for (util.List<VarDecl> vars = n.varList; vars != null; vars = vars.tail){
			LlvmValue variable = vars.head.accept(this);
			
			System.out.format("variable: %s\n",variable);
			
			varList.add(variable);
			typeList.add((LlvmType) vars.head.type.accept(this));
		}
		
		/*da problema!
		for(i=0;i<varListSize;i++){
			LlvmValue variable = n.varList.head.accept(this);
			
			System.out.format("variable: %s\n",variable);
			
			varList.add(n.varList.head.accept(this));
			typeList.add((LlvmType) n.varList.head.type.accept(this));
			
			n.varList = n.varList.tail;
		}*/
			
		
	}
	
	System.out.format("@SymTab escrevendo no classes: \n%s \n%s \n%s\n",n.name.s, new LlvmStructure(typeList), varList);
	//cria classnode referente a classe atual. Sera utilizado na hora de guardar os metodos
	ClassNode classe_atual = new ClassNode(n.name.s, new LlvmStructure(typeList), varList);
	
	//adiciona a classe ao mapa de classes
	classes.put(n.name.s, classe_atual);
	
	//vai ser utilizado na parte de metodos
	classEnv = classe_atual;
	
	if(n.methodList != null) {
		j = n.methodList.size();
		//itera todos os metodos da classe
		
		//Retirado do exemplo da main...
		// Percorre n.methodList visitando cada método
		for (util.List<MethodDecl> method = n.methodList; method != null; method = method.tail){
			System.out.format("@SymTab class - method: %s ", method);
			method.head.accept(this);
		}
		
	}
	
	return null;
}

//provavelmente nao vamos tratar
	public LlvmValue visit(ClassDeclExtends n){
		System.out.format("@SymTabClassDeclExtends...\n");
		return null;}
	public LlvmValue visit(VarDecl n){
		System.out.format("@SymTabVarDecl...\n");
		
		//var type
		LlvmType type = (LlvmType)n.type.accept(this);
		
		StringBuilder name = new StringBuilder();
		
		name.append("%");
		name.append(n.name.s);
		name.append("_address");
		
		LlvmNamedValue var = new LlvmNamedValue(name.toString(),type);
		
		return var;
		//return null;
		
	}
	public LlvmValue visit(Formal n){
		System.out.format("@SymTabFormal...\n");
		
		//var type
		LlvmType type = (LlvmType)n.type.accept(this);
				
		StringBuilder name = new StringBuilder();
				
		name.append("%");
		name.append(n.name.s);
		name.append("_address");
				
		LlvmNamedValue var = new LlvmNamedValue(name.toString(),type);
				
		return var;
		//return null;
	}
	public LlvmValue visit(MethodDecl n){
		System.out.format("@SymTabMethodDecl...\n");
		
		System.out.format("n.name.s: %s\n", n.name.s);
		
		int i,j;
		
		//parametros da SymTab
		List<String> formals_name = new LinkedList<String>();;
		List<LlvmValue> formals_value = new LinkedList<LlvmValue>();;
		List<String> locals_name = new LinkedList<String>();;
		List<LlvmValue> locals_value = new LinkedList<LlvmValue>();;
		
		//preenchendo a lista de formals do metodo
		if(n.formals != null) {
			System.out.format("formals: %s\n",n.formals.head);
			j = n.formals.size();
			System.out.format("formals size: %d\n", j);
			
			for (util.List<Formal> formals = n.formals; formals != null; formals = formals.tail){
				System.out.format("@SymTab method - formals: %s ", formals);
				
				LlvmValue param = formals.head.accept(this);
				
				System.out.format("@SymTab formals head: %s \n", n.formals.head);
				System.out.format("@SymTab formals head value: %s \n", param);
				
				//Grava nas listas os nomes+values dos formals do metodo
				formals_name.add(formals.head.name.toString());
				formals_value.add(param);
			}
			
			
			/*desse jeito da problema :(
			//itera todos os parametros do metodo
			for (i = 0; i < j; i++) {
				
				util.List<Formal> param = n.formals;
				
				System.out.format("@SymTab formals head: %s \n", n.formals.head);
				System.out.format("@SymTab formals head value: %s \n", param);
				
				//Grava nas listas os nomes+values dos formals do metodo
				formals_name.add(n.formals.head.name.toString());
				formals_value.add(param);
				
				n.formals = n.formals.tail;
				
			}*/
		}
		
		//Preenchendo a lista de locals do metodo
		if(n.locals!=null){
			System.out.format("locals size: %s\n",n.locals.size());
			j = n.locals.size();
			
			for (util.List<VarDecl> locals = n.locals; locals != null; locals = locals.tail){
				System.out.format("@SymTab method - locals: %s ", locals);
				
				LlvmValue param2 = locals.head.accept(this);
				
				System.out.format("@SymTab locals head: %s \n", n.locals.head);
				System.out.format("@SymTab locals head value: %s \n", param2);
				
				//Grava nas listas os nomes+values dos formals do metodo
				locals_name.add(locals.head.name.toString());
				locals_value.add(param2);
			}
			/*desse jeito da problema
			
			for(i=0;i<j;i++){
				
				//chama varDecl para cada variavel local
				LlvmValue locals_var = n.locals.head.accept(this);
				
				System.out.format("@SymTab locals head: %s \n", n.locals.head);
				System.out.format("@SymTab locals head value: %s \n", locals_var);
				
				//Grava nas listas os nomes+values dos formals do metodo
				locals_name.add(n.locals.head.name.toString());
				locals_value.add(locals_var);
				
				n.locals = n.locals.tail;
				
			}*/
		}
		
		System.out.format("escrevendo no methods: \n%s \n%s \n%s \n%s \n%s\n",n.name.s,formals_name,formals_value,locals_name,locals_value);
		
		MethodNode method_atual = new MethodNode(n.name.s,formals_name,formals_value,locals_name,locals_value);
		
		//adiciona metodo ao methods
		methods.put(n.name.s,method_atual);
		
		//classEnv.methods.put(n.name.s, method_atual);
		
		return null;
		}
	
	public LlvmValue visit(IdentifierType n){
		System.out.format("@SymTabIdentifierType...\n");
		System.out.format("identifiertype :)\n");
		
		//%class.name
		StringBuilder name = new StringBuilder();
		
		//name.append("%class.");
		name.append(n.name);
		
		System.out.format("name: %s\n",name.toString());
		
		//cria classType
		LlvmClassInfo classType = new LlvmClassInfo(name.toString());
		
		//%class.name *
		//LlvmPointer classTypePtr = new LlvmPointer(classType);
		
		//return classTypePtr;		

		return classType;
		//return null;
		
		}
	public LlvmValue visit(IntArrayType n){
		System.out.format("@SymTabIntArrayType...\n");
		
		return LlvmPrimitiveType.I32PTR;
		//return null;
		}
	public LlvmValue visit(BooleanType n){
		System.out.format("@SymTabBooleanType...\n");
		
		return LlvmPrimitiveType.I1;
		//return null;
	}
	public LlvmValue visit(IntegerType n){
		System.out.format("@SymTabIntegerType...\n");
		
		return LlvmPrimitiveType.I32;
		
		//return null;
		}
}

class ClassNode extends LlvmType {
	public String nameClass;
	public LlvmStructure classType;
	public List<LlvmValue> varList;
	ClassNode (String nameClass, LlvmStructure classType, List<LlvmValue> varList){
		this.nameClass = nameClass;
		this.classType = classType;
		this.varList = varList;
	}
}

//nesta classe, armazenaremos todas as informacoes dos metodos.
//nome do metodo, variaveis locais e parametros.
class MethodNode extends LlvmType{
	
	public String name;
	
	//nome e value do formals
	
	public List<String> formals_name;
	public List<LlvmValue> formals_value;
	
	//nome e value do locals
	
	public List<String> locals_name;
	public List<LlvmValue> locals_value;
	
	MethodNode(String name, List<String> formals_name, List<LlvmValue> formals_value, List<String> locals_name, List<LlvmValue> locals_value){
		this.name = name;
		this.formals_name = formals_name;
		this.formals_value = formals_value;
		this.locals_name = locals_name;
		this.locals_value = locals_value;
	}
}




