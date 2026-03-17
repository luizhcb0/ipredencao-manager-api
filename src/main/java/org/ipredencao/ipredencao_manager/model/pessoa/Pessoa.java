package org.ipredencao.ipredencao_manager.model.pessoa;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.ipredencao.ipredencao_manager.model.endereco.Endereco;
import org.ipredencao.ipredencao_manager.model.pessoa.relacionamento_pessoa.Relacionamento;
import org.joda.time.DateTime;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Pessoa {
    private Long id;
    private String nome;
    private String apelido;
    private String email;
    private List<String> emailsSecundarios;
    private String telefone;
    private List<String> telefonesSecundarios;
    private String campus;
    private DateTime dataNascimento;
    private DateTime dataFalecimento;
    private String cpf;
    private String rg;
    private EstadoCivil estadoCivil;
    private String igrejaAnterior;
    private String situacaoIgrejaAnterior;
    private String tempoNaIgreja;
    private String motivosParaAdmissao;
    private TipoBatismo tipoBatismo;
    private DateTime dataBatismo;
    private DateTime dataProfissaoDeFe;
    private String igrejaBatismo;
    private List<String> profissao;
    private List<String> empresa;
    private Endereco endereco;
    private String informacoesAdicionais;
    private String fotoUrl;
    private Sexo sexo;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Long chefeDeFamiliaId;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ChefeDeFamiliaRef chefeDeFamilia;
    private CategoriaEnum categoria;
    private Boolean bookmark;
    private Long updatedByUserId;

    // Relacionamento qualificados com outras pessoas
    private List<Relacionamento> relacionamentos;

    // getters e setters
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
    public DateTime getDataFalecimento() { return dataFalecimento; }
    public void setDataFalecimento(DateTime dataFalecimento) { this.dataFalecimento = dataFalecimento; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getRg() { return rg; }
    public void setRg(String rg) { this.rg = rg; }
    public EstadoCivil getEstadoCivil() { return estadoCivil; }
    public void setEstadoCivil(EstadoCivil estadoCivil) { this.estadoCivil = estadoCivil; }
    public String getIgrejaAnterior() { return igrejaAnterior; }
    public void setIgrejaAnterior(String igrejaAnterior) { this.igrejaAnterior = igrejaAnterior; }
    public String getSituacaoIgrejaAnterior() { return situacaoIgrejaAnterior; }
    public void setSituacaoIgrejaAnterior(String situacaoIgrejaAnterior) { this.situacaoIgrejaAnterior = situacaoIgrejaAnterior; }
    public String getTempoNaIgreja() { return tempoNaIgreja; }
    public void setTempoNaIgreja(String tempoNaIgreja) { this.tempoNaIgreja = tempoNaIgreja; }
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
    public Endereco getEndereco() { return endereco; }
    public void setEndereco(Endereco endereco) { this.endereco = endereco; }
    public String getInformacoesAdicionais() { return informacoesAdicionais; }
    public void setInformacoesAdicionais(String informacoesAdicionais) { this.informacoesAdicionais = informacoesAdicionais; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public Sexo getSexo() { return sexo; }
    public void setSexo(Sexo sexo) { this.sexo = sexo; }
    public Long getChefeDeFamiliaId() { return chefeDeFamiliaId; }
    public void setChefeDeFamiliaId(Long chefeDeFamiliaId) { this.chefeDeFamiliaId = chefeDeFamiliaId; }
    public ChefeDeFamiliaRef getChefeDeFamilia() { return chefeDeFamilia; }
    public void setChefeDeFamilia(ChefeDeFamiliaRef chefeDeFamilia) { this.chefeDeFamilia = chefeDeFamilia; }
    public CategoriaEnum getCategoria() { return categoria; }
    public void setCategoria(CategoriaEnum categoria) { this.categoria = categoria; }
    public Boolean getBookmark() { return bookmark; }
    public void setBookmark(Boolean bookmark) { this.bookmark = bookmark; }
    public List<Relacionamento> getRelacionamentos() { return relacionamentos; }
    public void setRelacionamentos(List<Relacionamento> relacionamentos) { this.relacionamentos = relacionamentos; }
    public Long getUpdatedByUserId() { return updatedByUserId; }
    public void setUpdatedByUserId(Long updatedByUserId) { this.updatedByUserId = updatedByUserId; }
}
