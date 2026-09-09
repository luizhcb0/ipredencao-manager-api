package org.ipredencao.ipredencao_manager.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.firebase.auth.FirebaseAuthException;
import org.ipredencao.ipredencao_manager.model.auth.PerfilAcesso;
import org.ipredencao.ipredencao_manager.model.formulario_pessoa.FormularioPessoa;
import org.ipredencao.ipredencao_manager.model.formulario_pessoa.ProcessarFormularioRequest;
import org.ipredencao.ipredencao_manager.model.pessoa.CategoriaEnum;
import org.ipredencao.ipredencao_manager.model.pessoa.Pessoa;
import org.ipredencao.ipredencao_manager.model.pessoa.Sexo;
import org.ipredencao.ipredencao_manager.model.user.Usuario;
import org.ipredencao.ipredencao_manager.model.user.UsuarioQuery;
import org.ipredencao.ipredencao_manager.repository.UsuarioRepository;
import org.ipredencao.ipredencao_manager.service.FirebaseAuthService;
import org.ipredencao.ipredencao_manager.service.PessoaService;
import org.ipredencao.ipredencao_manager.service.UserService;
import org.ipredencao.ipredencao_manager.service.firebase.FirebaseUser;
import org.ipredencao.ipredencao_manager.support.IntegrationTestBase;
import org.ipredencao.ipredencao_manager.support.PessoaFixture;
import org.ipredencao.ipredencao_manager.support.UsuarioFixture;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class FormularioPessoaControllerIT extends IntegrationTestBase {

    private static final String BASE = "/api/formulario-pessoa";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PessoaService pessoaService;

    @MockitoBean
    private FirebaseAuthService firebaseAuthService;

    @BeforeEach
    void setUp() throws FirebaseAuthException {
        when(firebaseAuthService.createUserWithoutPassword(any(), any()))
            .thenReturn(new FirebaseUser("form-firebase-uid", "captacao@exemplo.com", "Nome", false, null, true));
        doNothing().when(firebaseAuthService).setUserDisabled(any(), eq(true));
        doNothing().when(firebaseAuthService).deleteUser(any());
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void findById_includesUpdatedAtFromDatabase() throws Exception {
        FormularioPessoa created = read(mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(publicFormBody("Formulário Timestamp", "captacao@exemplo.com", null)))
                .andExpect(status().isOk()), FormularioPessoa.class);

        FormularioPessoa fetched = read(mockMvc.perform(get(BASE + "/{id}", created.getId()))
                .andExpect(status().isOk()), FormularioPessoa.class);
        assertNotNull(fetched.getUpdatedAt());
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void process_createsInactiveMemberUser() throws Exception {
        long formId = createForm("Membro Novo", "membro@exemplo.com", CategoriaEnum.MEMBRO_COMUNGANTE);

        process(formId, null)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensagem").value((Object) null))
            .andExpect(jsonPath("$.pessoaPrincipal.id").isNumber());

        Usuario user = findUserByEmail("membro@exemplo.com");
        assertNotNull(user);
        assertFalse(user.getActive());
        assertEquals(PerfilAcesso.MEMBER, user.getAccessProfile());
        verify(firebaseAuthService).setUserDisabled("form-firebase-uid", true);
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void process_createsInactiveMembershipCandidate() throws Exception {
        long formId = createForm("Admitendo Novo", "admitendo@exemplo.com", CategoriaEnum.AGUARDANDO_ENTREVISTA);

        process(formId, null).andExpect(status().isOk());

        Usuario user = findUserByEmail("admitendo@exemplo.com");
        assertNotNull(user);
        assertEquals(PerfilAcesso.MEMBERSHIP_CANDIDATE, user.getAccessProfile());
        assertFalse(user.getActive());
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void process_skipsUserForIneligibleCategory() throws Exception {
        long formId = createForm("Pastor Novo", "pastor@exemplo.com", CategoriaEnum.PASTOR_DA_IGREJA);

        process(formId, null).andExpect(status().isOk());

        assertNull(findUserByEmail("pastor@exemplo.com"));
        verify(firebaseAuthService, never()).createUserWithoutPassword(any(), any());
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void process_createsUserWhenEnrichingExistingPerson() throws Exception {
        Pessoa pessoa = PessoaFixture.membroComungante(pessoaService, "Pessoa Sem Email", Sexo.FEMININO);
        long formId = createForm("Pessoa Sem Email", "enriquecido@exemplo.com", CategoriaEnum.MEMBRO_COMUNGANTE);

        process(formId, pessoa.getId())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensagem").value((Object) null));

        Usuario user = findUserByEmail("enriquecido@exemplo.com");
        assertNotNull(user);
        assertEquals(pessoa.getId(), user.getPersonId());
        assertEquals(PerfilAcesso.MEMBER, user.getAccessProfile());
        assertFalse(user.getActive());
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void process_doesNotDuplicateAlreadyLinkedUser() throws Exception {
        Pessoa pessoa = PessoaFixture.builder(pessoaService)
            .nome("Já Vinculada")
            .sexo(Sexo.MASCULINO)
            .email("ja@exemplo.com")
            .categoria(CategoriaEnum.MEMBRO_COMUNGANTE)
            .build();
        usuarioRepository.insert(UsuarioFixture.builder()
            .email("ja@exemplo.com")
            .personId(pessoa.getId())
            .accessProfile(PerfilAcesso.BOLETIM)
            .active(true)
            .build());

        long formId = createForm("Já Vinculada", "ja@exemplo.com", CategoriaEnum.MEMBRO_COMUNGANTE);

        process(formId, pessoa.getId()).andExpect(status().isOk());

        Usuario user = findUserByPersonId(pessoa.getId());
        assertEquals(PerfilAcesso.BOLETIM, user.getAccessProfile());
        assertEquals(Boolean.TRUE, user.getActive());
        verify(firebaseAuthService, never()).createUserWithoutPassword(any(), any());
    }

    @Test
    @WithMockUser(roles = "DIACONO")
    void process_returnsWarningWhenFirebaseFails() throws Exception {
        when(firebaseAuthService.createUserWithoutPassword(any(), any()))
            .thenThrow(new RuntimeException("Firebase indisponível"));

        long formId = createForm("Falha Firebase", "falha@exemplo.com", CategoriaEnum.MEMBRO_NAO_COMUNGANTE);

        String body = process(formId, null)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensagem").value(UserService.USER_PROVISION_WARNING))
            .andExpect(jsonPath("$.pessoaPrincipal.id").isNumber())
            .andReturn().getResponse().getContentAsString();

        long pessoaId = objectMapper.readTree(body).at("/pessoaPrincipal/id").asLong();
        assertNotNull(pessoaService.findById(pessoaId));
        assertNull(findUserByEmail("falha@exemplo.com"));
    }

    private ResultActions process(long formId, Long pessoaId) throws Exception {
        return mockMvc.perform(post(BASE + "/processar")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new ProcessarFormularioRequest(formId, pessoaId))));
    }

    private <T> T read(ResultActions actions, Class<T> type) throws Exception {
        return objectMapper.readValue(actions.andReturn().getResponse().getContentAsString(), type);
    }

    private Usuario findUserByEmail(String email) {
        return usuarioRepository.find(UsuarioQuery.builder().email(email).build())
            .stream().findFirst().orElse(null);
    }

    private Usuario findUserByPersonId(Long personId) {
        return usuarioRepository.find(UsuarioQuery.builder().personId(personId).build())
            .stream().findFirst().orElse(null);
    }

    private long createForm(String nome, String email, CategoriaEnum categoria) throws Exception {
        String created = mockMvc.perform(post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(publicFormBody(nome, email, categoria)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(created).get("id").asLong();
    }

    /** Só os campos NOT NULL da tabela, mais categoria quando o processamento cria pessoa. */
    private String publicFormBody(String nome, String email, CategoriaEnum categoria) throws Exception {
        FormularioPessoa form = new FormularioPessoa();
        form.setNome(nome);
        form.setSexo(Sexo.FEMININO);
        form.setEmail(email);
        form.setTelefone("81999990000");
        form.setCampus("SEDE");
        form.setDataNascimento(DateTime.now().minusYears(30).withTimeAtStartOfDay());
        form.setCpf("00000000000");
        form.setRg("0000000");
        form.setEnderecoCep("50000-000");
        form.setEnderecoLogradouro("Rua da Captação");
        form.setEnderecoNumero("100");
        ObjectNode node = (ObjectNode) objectMapper.valueToTree(form);
        if (categoria != null) {
            node.put("categoria", categoria.getId());
        }
        return objectMapper.writeValueAsString(node);
    }
}
