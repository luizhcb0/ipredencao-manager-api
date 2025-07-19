package org.ipredencao.ipredencao_manager.model;

import org.joda.time.DateTime;
import java.util.List;

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
    private String profissao;
    private String empresa;
    private String endereco;
    private Regiao regiao;
    private Double latitude;
    private Double longitude;
    private String fotoUrl;
    private Status status;
    private Sexo sexo;
    private Long chefeDeFamiliaId;

    // Relacionamentos qualificados com outras pessoas
    private List<RelacionamentoPessoa> relacionamentos;

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
    public String getProfissao() { return profissao; }
    public void setProfissao(String profissao) { this.profissao = profissao; }
    public String getEmpresa() { return empresa; }
    public void setEmpresa(String empresa) { this.empresa = empresa; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public Regiao getRegiao() { return regiao; }
    public void setRegiao(Regiao regiao) { this.regiao = regiao; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Sexo getSexo() { return sexo; }
    public void setSexo(Sexo sexo) { this.sexo = sexo; }
    public Long getChefeDeFamiliaId() { return chefeDeFamiliaId; }
    public void setChefeDeFamiliaId(Long chefeDeFamiliaId) { this.chefeDeFamiliaId = chefeDeFamiliaId; }
    public List<RelacionamentoPessoa> getRelacionamentos() { return relacionamentos; }
    public void setRelacionamentos(List<RelacionamentoPessoa> relacionamentos) { this.relacionamentos = relacionamentos; }
} 