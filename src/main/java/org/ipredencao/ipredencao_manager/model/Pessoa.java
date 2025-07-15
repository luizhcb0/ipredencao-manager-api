package org.ipredencao.ipredencao_manager.model;

import org.joda.time.DateTime;
import java.util.List;

public class Pessoa {
    private Long id;
    private String nome;
    private String sedeCongregacao;
    private String apelido;
    private DateTime dataNascimento;
    private String telefone;
    private EstadoCivil estadoCivil;
    private String igrejaAnterior;
    private String situacaoIgrejaAnterior;
    private String tempoNaIpr;
    private String motivosAdmissao;
    private TipoBatismo tipoBatismo;
    private DateTime dataBatismo;
    private String igrejaBatismo;
    private String dadosOficial;
    private String supervisoes;
    private String outrasCategorias;
    private EstadoPessoa estadoPessoa;
    private TipoAdmissao tipoAdmissao;
    private String cpf;
    private String rg;
    private String emailAdicional;
    private String telefoneAdicional;
    private String emailTrabalho;
    private String telefoneTrabalho;
    private String profissao;
    private String empresa;
    private String endereco;
    private Double latitude;
    private Double longitude;
    private String regiaoGf;
    private String skype;
    private String twitter;
    private String linkedin;
    private String instagram;
    private String facebook;
    private String paginaPessoal;
    private String fotoUrl;

    // Relacionamentos qualificados com outras pessoas
    private List<RelacionamentoPessoa> relacionamentos;

    // getters e setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getSedeCongregacao() { return sedeCongregacao; }
    public void setSedeCongregacao(String sedeCongregacao) { this.sedeCongregacao = sedeCongregacao; }
    public String getApelido() { return apelido; }
    public void setApelido(String apelido) { this.apelido = apelido; }
    public DateTime getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(DateTime dataNascimento) { this.dataNascimento = dataNascimento; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public EstadoCivil getEstadoCivil() { return estadoCivil; }
    public void setEstadoCivil(EstadoCivil estadoCivil) { this.estadoCivil = estadoCivil; }
    public String getIgrejaAnterior() { return igrejaAnterior; }
    public void setIgrejaAnterior(String igrejaAnterior) { this.igrejaAnterior = igrejaAnterior; }
    public String getSituacaoIgrejaAnterior() { return situacaoIgrejaAnterior; }
    public void setSituacaoIgrejaAnterior(String situacaoIgrejaAnterior) { this.situacaoIgrejaAnterior = situacaoIgrejaAnterior; }
    public String getTempoNaIpr() { return tempoNaIpr; }
    public void setTempoNaIpr(String tempoNaIpr) { this.tempoNaIpr = tempoNaIpr; }
    public String getMotivosAdmissao() { return motivosAdmissao; }
    public void setMotivosAdmissao(String motivosAdmissao) { this.motivosAdmissao = motivosAdmissao; }
    public TipoBatismo getTipoBatismo() { return tipoBatismo; }
    public void setTipoBatismo(TipoBatismo tipoBatismo) { this.tipoBatismo = tipoBatismo; }
    public DateTime getDataBatismo() { return dataBatismo; }
    public void setDataBatismo(DateTime dataBatismo) { this.dataBatismo = dataBatismo; }
    public String getIgrejaBatismo() { return igrejaBatismo; }
    public void setIgrejaBatismo(String igrejaBatismo) { this.igrejaBatismo = igrejaBatismo; }
    public String getDadosOficial() { return dadosOficial; }
    public void setDadosOficial(String dadosOficial) { this.dadosOficial = dadosOficial; }
    public String getSupervisoes() { return supervisoes; }
    public void setSupervisoes(String supervisoes) { this.supervisoes = supervisoes; }
    public String getOutrasCategorias() { return outrasCategorias; }
    public void setOutrasCategorias(String outrasCategorias) { this.outrasCategorias = outrasCategorias; }
    public EstadoPessoa getEstadoPessoa() { return estadoPessoa; }
    public void setEstadoPessoa(EstadoPessoa estadoPessoa) { this.estadoPessoa = estadoPessoa; }
    public TipoAdmissao getTipoAdmissao() { return tipoAdmissao; }
    public void setTipoAdmissao(TipoAdmissao tipoAdmissao) { this.tipoAdmissao = tipoAdmissao; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getRg() { return rg; }
    public void setRg(String rg) { this.rg = rg; }
    public String getEmailAdicional() { return emailAdicional; }
    public void setEmailAdicional(String emailAdicional) { this.emailAdicional = emailAdicional; }
    public String getTelefoneAdicional() { return telefoneAdicional; }
    public void setTelefoneAdicional(String telefoneAdicional) { this.telefoneAdicional = telefoneAdicional; }
    public String getEmailTrabalho() { return emailTrabalho; }
    public void setEmailTrabalho(String emailTrabalho) { this.emailTrabalho = emailTrabalho; }
    public String getTelefoneTrabalho() { return telefoneTrabalho; }
    public void setTelefoneTrabalho(String telefoneTrabalho) { this.telefoneTrabalho = telefoneTrabalho; }
    public String getProfissao() { return profissao; }
    public void setProfissao(String profissao) { this.profissao = profissao; }
    public String getEmpresa() { return empresa; }
    public void setEmpresa(String empresa) { this.empresa = empresa; }
    public String getEndereco() { return endereco; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public String getRegiaoGf() { return regiaoGf; }
    public void setRegiaoGf(String regiaoGf) { this.regiaoGf = regiaoGf; }
    public String getSkype() { return skype; }
    public void setSkype(String skype) { this.skype = skype; }
    public String getTwitter() { return twitter; }
    public void setTwitter(String twitter) { this.twitter = twitter; }
    public String getLinkedin() { return linkedin; }
    public void setLinkedin(String linkedin) { this.linkedin = linkedin; }
    public String getInstagram() { return instagram; }
    public void setInstagram(String instagram) { this.instagram = instagram; }
    public String getFacebook() { return facebook; }
    public void setFacebook(String facebook) { this.facebook = facebook; }
    public String getPaginaPessoal() { return paginaPessoal; }
    public void setPaginaPessoal(String paginaPessoal) { this.paginaPessoal = paginaPessoal; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public List<RelacionamentoPessoa> getRelacionamentos() { return relacionamentos; }
    public void setRelacionamentos(List<RelacionamentoPessoa> relacionamentos) { this.relacionamentos = relacionamentos; }
} 