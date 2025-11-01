package org.ipredencao.ipredencao_manager.model.formulario_pessoa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.ipredencao.ipredencao_manager.model.pessoa.EstadoCivil;
import org.ipredencao.ipredencao_manager.model.pessoa.FormPessoaStatus;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.TipoBatismo;
import org.joda.time.DateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FormularioPessoa {
    private Long id;
    private String nome;
    private String apelido;
    private String email;
    private List<String> emailsSecundarios;
    private String telefone;
    private List<String> telefonesSecundarios;
    private String campus;
    private DateTime dataNascimento;
    private String cpf;
    private String rg;
    private EstadoCivil estadoCivil;
    private String igrejaAnterior;
    private String motivosParaAdmissao;
    private TipoBatismo tipoBatismo;
    private DateTime dataBatismo;
    private DateTime dataProfissaoDeFe;
    private String igrejaBatismo;
    private List<String> profissao;
    private List<String> empresa;
    private String enderecoCep;
    private String enderecoLogradouro;
    private String enderecoNumero;
    private String enderecoComplemento;
    private String fotoUrl;
    private Sexo sexo;
    private String chefeDeFamilia;
    private Boolean propagarEnderecoChefeFamilia;
    private Long pessoaId;
    private CategoriaEnum categoria;
    private FormPessoaStatus status;
    // Campos extras do formulário
    private String nomePessoaRelacionada;
    private DateTime inicioRelacionamento;
    private String nomePai;
    private String nomeMae;
    private List<String> nomeFilhos;

    // Getters e setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getApelido() { return apelido; }
    public void setApelido(String apelido) { this.apelido = apelido; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public List<String> getEmailsSecundarios() { return emailsSecundarios; }
    public void setEmailsSecundarios(List<String> emailsSecundarios) { this.emailsSecundarios = emailsSecundarios; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public List<String> getTelefonesSecundarios() { return telefonesSecundarios; }
    public void setTelefonesSecundarios(List<String> telefonesSecundarios) { this.telefonesSecundarios = telefonesSecundarios; }
    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }
    public DateTime getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(DateTime dataNascimento) { this.dataNascimento = dataNascimento; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getRg() { return rg; }
    public void setRg(String rg) { this.rg = rg; }
    public EstadoCivil getEstadoCivil() { return estadoCivil; }
    public void setEstadoCivil(EstadoCivil estadoCivil) { this.estadoCivil = estadoCivil; }
    public String getIgrejaAnterior() { return igrejaAnterior; }
    public void setIgrejaAnterior(String igrejaAnterior) { this.igrejaAnterior = igrejaAnterior; }
    public String getMotivosParaAdmissao() { return motivosParaAdmissao; }
    public void setMotivosParaAdmissao(String motivosParaAdmissao) { this.motivosParaAdmissao = motivosParaAdmissao; }
    public TipoBatismo getTipoBatismo() { return tipoBatismo; }
    public void setTipoBatismo(TipoBatismo tipoBatismo) { this.tipoBatismo = tipoBatismo; }
    public DateTime getDataBatismo() { return dataBatismo; }
    public void setDataBatismo(DateTime dataBatismo) { this.dataBatismo = dataBatismo; }
    public DateTime getDataProfissaoDeFe() { return dataProfissaoDeFe; }
    public void setDataProfissaoDeFe(DateTime dataProfissaoDeFe) { this.dataProfissaoDeFe = dataProfissaoDeFe; }
    public String getIgrejaBatismo() { return igrejaBatismo; }
    public void setIgrejaBatismo(String igrejaBatismo) { this.igrejaBatismo = igrejaBatismo; }
    public List<String> getProfissao() { return profissao; }
    public void setProfissao(List<String> profissao) { this.profissao = profissao; }
    public List<String> getEmpresa() { return empresa; }
    public void setEmpresa(List<String> empresa) { this.empresa = empresa; }
    public String getEnderecoCep() { return enderecoCep; }
    public void setEnderecoCep(String enderecoCep) { this.enderecoCep = enderecoCep; }
    public String getEnderecoLogradouro() { return enderecoLogradouro; }
    public void setEnderecoLogradouro(String enderecoLogradouro) { this.enderecoLogradouro = enderecoLogradouro; }
    public String getEnderecoNumero() { return enderecoNumero; }
    public void setEnderecoNumero(String enderecoNumero) { this.enderecoNumero = enderecoNumero; }
    public String getEnderecoComplemento() { return enderecoComplemento; }
    public void setEnderecoComplemento(String enderecoComplemento) { this.enderecoComplemento = enderecoComplemento; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public Sexo getSexo() { return sexo; }
    public void setSexo(Sexo sexo) { this.sexo = sexo; }
    public String getChefeDeFamilia() { return chefeDeFamilia; }
    public void setChefeDeFamilia(String chefeDeFamilia) { this.chefeDeFamilia = chefeDeFamilia; }
    public Boolean getPropagarEnderecoChefeFamilia() { return propagarEnderecoChefeFamilia; }
    public void setPropagarEnderecoChefeFamilia(Boolean propagarEnderecoChefeFamilia) { this.propagarEnderecoChefeFamilia = propagarEnderecoChefeFamilia; }
    public Long getPessoaId() { return pessoaId; }
    public void setPessoaId(Long pessoaId) { this.pessoaId = pessoaId; }
    public CategoriaEnum getCategoria() { return categoria; }
    public void setCategoria(CategoriaEnum categoria) { this.categoria = categoria; }
    public FormPessoaStatus getStatus() { return status; }
    public void setStatus(FormPessoaStatus status) { this.status = status; }
    public String getNomePessoaRelacionada() { return nomePessoaRelacionada; }
    public void setNomePessoaRelacionada(String nomePessoaRelacionada) { this.nomePessoaRelacionada = nomePessoaRelacionada; }
    public DateTime getInicioRelacionamento() { return inicioRelacionamento; }
    public void setInicioRelacionamento(DateTime inicioRelacionamento) { this.inicioRelacionamento = inicioRelacionamento; }
    public String getNomePai() { return nomePai; }
    public void setNomePai(String nomePai) { this.nomePai = nomePai; }
    public String getNomeMae() { return nomeMae; }
    public void setNomeMae(String nomeMae) { this.nomeMae = nomeMae; }
    public List<String> getNomeFilhos() { return nomeFilhos; }
    public void setNomeFilhos(List<String> nomeFilhos) { this.nomeFilhos = nomeFilhos; }
}