def errors = new ArrayList<Map>()

def addError = { String field, String message -> 
    errors.add(["campo": "${field}", "mensagem": message]) 
}

// Função auxiliar para verificar se objeto está realmente vazio/nulo
def isNullOrEmpty = { obj ->
    obj == null || 
    (obj instanceof String && (obj == "null" || obj.trim().isEmpty())) ||
    (obj instanceof Map && obj.isEmpty()) ||
    (obj instanceof List && obj.isEmpty()) ||
    (obj instanceof Boolean && obj == false) // se for relevante para seu caso
}

// Definição de campos obrigatórios
def camposObrigatoriosOfertaPacote = [
    "codigoPacote",
    "nomePacote",
    "valorPacote"
]

def camposObrigatoriosOfertaCartao = [
    "\$.bandeira_cartao",
    "\$.codigo_fatura_digital_cartao",
    "\$.codigo_tipo_cartao",
    "\$.dia_vencimento_cartao",
    "\$.dn_cartao_credito",
    "\$.dn_cartao_debito",
    "\$.indicador_debito_automatico",
    "\$.indicador_oferta_fatura",
    "\$.indicador_opcao_seguro_cartao",
    "\$.indicador_overlimit",
    "\$.indicador_programa_recompensa",
    "\$.numero_de_parcela",
    "\$.tipo_cartao",
    "\$.valor_da_parcela",
    "\$.valor_maximo_cartao",
    "\$.valor_minimo_cartao",
    "\$.valor_total_anuidade_cartao"
]

def ofertaCartaoNpcCaminhosObrigatorios = [
    "\$.data_vencimento",
    "\$.opcoes_produtos_disponiveis[?(@.id_oferta!=null)].id_oferta",
    "\$.opcoes_produtos_disponiveis[0].dados_produto[?(@.id=='ofertas')].id",
    "\$.opcoes_produtos_disponiveis[0].dados_produto[?(@.id=='ofertas')].valor[?(@.id!=null)].id",
    "\$.opcoes_produtos_disponiveis[0].dados_produto[?(@.id=='ofertas')].valor[0].produtos[?(@.idProduto!=null)].idProduto",
    "\$.opcoes_produtos_disponiveis[0].dados_produto[?(@.id=='ofertas')].valor[0].produtos[0].dadosProduto.planos[?(@.idPlano!=null)].idPlano",
    "\$.opcoes_produtos_disponiveis[0].dados_produto[?(@.id=='ofertas')].valor[0].produtos[0].dadosProduto.dn",
    "\$.opcoes_produtos_disponiveis[0].dados_produto[?(@.id=='canal')].valor.canalOrigem",
    "\$.opcoes_produtos_disponiveis[0].dados_produto[?(@.id=='canal')].valor.subcanalOrigem",
    "\$.opcoes_produtos_disponiveis[0].dados_produto[?(@.id=='idIntencao')].valor"
]

// Obtém as variáveis
def ofertaCartao = execution.getVariable("oferta_cartao")
def ofertaCartaoNpc = execution.getVariable("oferta_cartao_npc")

// Validação de oferta_cartao / oferta_cartao_npc (exclusividade)
def ofertaCartaoValida = !isNullOrEmpty(ofertaCartao)
def ofertaCartaoNpcValida = !isNullOrEmpty(ofertaCartaoNpc)

if ((!ofertaCartaoValida && !ofertaCartaoNpcValida) || (ofertaCartaoValida && ofertaCartaoNpcValida)) {
    addError("oferta_cartao ou oferta_cartao_npc", "Exatamente uma das variáveis oferta_cartao ou oferta_cartao_npc deve ser fornecida.")
} else if (ofertaCartaoNpcValida) {
    // Validação do oferta_cartao_npc
    ofertaCartaoNpcCaminhosObrigatorios.each { caminho ->
        try {
            def jsonObj = JSON(ofertaCartaoNpc)
            def elements = jsonObj.jsonPath(caminho).with {
                try {
                    it.elementList()
                } catch (Exception ignored) {
                    [it.element()]
                }
            }

            if (elements.isEmpty()) {
                addError(caminho, "Campo obrigatorio nao encontrado.")
            } else if (elements.size() > 1) {
                addError(caminho, "Campo obrigatorio contem multiplos valores, mas apenas um e permitido.")
            } else {
                def element = elements[0]
                if (element.isNull() || (element.isString() && element.stringValue().trim().isEmpty())) {
                    addError(caminho, "Campo obrigatorio esta vazio.")
                }
            }
        } catch (Exception e) {
            addError(caminho, "Campo obrigatorio nao encontrado. Erro: ${e.message}")
        }
    }
} else if (ofertaCartaoValida) {
    // Validação do oferta_cartao (caso precise validar os campos específicos)
    camposObrigatoriosOfertaCartao.each { campo ->
        try {
            def jsonObj = JSON(ofertaCartao)
            def elements = jsonObj.jsonPath(campo).with {
                try {
                    it.elementList()
                } catch (Exception ignored) {
                    [it.element()]
                }
            }

            if (elements.isEmpty()) {
                addError(campo, "Campo obrigatorio nao encontrado.")
            } else if (elements.size() > 1) {
                addError(campo, "Campo obrigatorio contem multiplos valores, mas apenas um e permitido.")
            } else {
                def element = elements[0]
                if (element.isNull() || (element.isString() && element.stringValue().trim().isEmpty())) {
                    addError(campo, "Campo obrigatorio esta vazio.")
                }
            }
        } catch (Exception e) {
            addError(campo, "Campo obrigatorio nao encontrado. Erro: ${e.message}")
        }
    }
}

// Validação de oferta_produtos
def ofertaProdutos = execution.getVariable("oferta_produtos")
if (isNullOrEmpty(ofertaProdutos)) {
    addError("oferta_produtos", "Campo obrigatório")
} else if (ofertaProdutos instanceof Map) {
    def aplicaut = ofertaProdutos["aplicaut"]
    if (aplicaut != null && aplicaut instanceof Map) {
        def indicadorContratar = aplicaut["indicador_contratar"]
        if (indicadorContratar == null) {
            addError("aplicaut.indicador_contratar", "Campo obrigatório")
        } else if (!(indicadorContratar instanceof Boolean)) {
            addError("aplicaut.indicador_contratar", "Deve ser boolean")
        }
    } else {
        addError("oferta_produtos.aplicaut", "Estrutura aplicaut não encontrada ou inválida")
    }
}

// Validação de oferta_pacote
def ofertaPacote = execution.getVariable("oferta_pacote")
if (isNullOrEmpty(ofertaPacote)) {
    addError("oferta_pacote", "Campo obrigatório")
} else if (ofertaPacote instanceof Map) {
    camposObrigatoriosOfertaPacote.each { campo ->
        if (!ofertaPacote.containsKey(campo) || ofertaPacote[campo] == null) {
            addError("oferta_pacote.${campo}", "Campo obrigatório")
        } else if (ofertaPacote[campo] instanceof String && ofertaPacote[campo].trim().isEmpty()) {
            addError("oferta_pacote.${campo}", "Campo obrigatório não pode ser vazio")
        }
    }
} else {
    addError("oferta_pacote", "Deve ser um objeto Map")
}

// Campos Obrigatórios Tarefa
def motorEfetivacaoIdOrigem = execution.getVariable("motor_efetivacao_id_origem")
if (isNullOrEmpty(motorEfetivacaoIdOrigem)) {
    addError("motor_efetivacao_id_origem", "Campo obrigatório")
}

// Verificação final e lançamento de erro BPMN
if (!errors.isEmpty()) {
    def errorMessage = new groovy.json.JsonBuilder(errors).toString()
    throw new org.camunda.bpm.engine.delegate.BpmnError("400", errorMessage)
}