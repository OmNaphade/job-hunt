package org.omnaphade.company_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.omnaphade.company_service.dtos.CompanyCreateDTO;
import org.omnaphade.company_service.dtos.CompanyResponseDTO;
import org.omnaphade.company_service.dtos.RecruiterDTO;
import org.omnaphade.company_service.entities.Company;
import org.omnaphade.company_service.entities.Recruiter;
import org.omnaphade.company_service.exception.DuplicateResourceException;
import org.omnaphade.company_service.exception.ResourceNotFoundException;
import org.omnaphade.company_service.repository.CompanyRepository;
import org.omnaphade.company_service.repository.RecruiterRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompanyServiceImplTest {

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private RecruiterRepository recruiterRepository;

    private CompanyServiceImpl companyService;

    @BeforeEach
    void setUp() {
        companyService = new CompanyServiceImpl(companyRepository, recruiterRepository);
    }

    private CompanyCreateDTO sampleCreateDTO() {
        return new CompanyCreateDTO("Acme Corp", "A widget company", "https://acme.example", "Remote");
    }

    private Company savedCompany() {
        return Company.builder()
                .id(1L)
                .name("Acme Corp")
                .description("A widget company")
                .website("https://acme.example")
                .location("Remote")
                .build();
    }

    private Recruiter savedRecruiter() {
        return Recruiter.builder()
                .id(10L)
                .userId(5L)
                .companyId(1L)
                .designation("Talent Lead")
                .verified(false)
                .build();
    }

    @Test
    void createCompany_whenNameNotTaken_persistsAndReturnsCompany() {
        when(companyRepository.existsByName("Acme Corp")).thenReturn(false);
        when(companyRepository.save(any(Company.class))).thenReturn(savedCompany());

        CompanyResponseDTO result = companyService.createCompany(sampleCreateDTO());

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Acme Corp");
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void createCompany_whenNameAlreadyTaken_throwsDuplicateResourceException() {
        when(companyRepository.existsByName("Acme Corp")).thenReturn(true);

        assertThatThrownBy(() -> companyService.createCompany(sampleCreateDTO()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Acme Corp");

        verify(companyRepository, never()).save(any(Company.class));
    }

    @Test
    void getCompanyById_found_returnsMappedCompany() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(savedCompany()));

        CompanyResponseDTO result = companyService.getCompanyById(1L);

        assertThat(result.getName()).isEqualTo("Acme Corp");
        assertThat(result.getWebsite()).isEqualTo("https://acme.example");
    }

    @Test
    void getCompanyById_notFound_throwsResourceNotFoundException() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.getCompanyById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getAllCompanies_returnsAllMappedCompanies() {
        when(companyRepository.findAll()).thenReturn(List.of(savedCompany()));

        List<CompanyResponseDTO> result = companyService.getAllCompanies();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Acme Corp");
    }

    @Test
    void updateCompany_whenFound_overwritesFieldsAndSaves() {
        Company existing = savedCompany();
        when(companyRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));

        CompanyCreateDTO dto = new CompanyCreateDTO("Acme International", "Now global", "https://acme.example", "Global");
        CompanyResponseDTO result = companyService.updateCompany(1L, dto);

        assertThat(result.getName()).isEqualTo("Acme International");
        assertThat(existing.getLocation()).isEqualTo("Global");
    }

    @Test
    void updateCompany_whenNotFound_throwsResourceNotFoundException() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.updateCompany(99L, sampleCreateDTO()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void deleteCompany_whenFound_deletesCompany() {
        Company existing = savedCompany();
        when(companyRepository.findById(1L)).thenReturn(Optional.of(existing));

        companyService.deleteCompany(1L);

        verify(companyRepository).delete(existing);
    }

    @Test
    void deleteCompany_whenNotFound_throwsAndNeverDeletes() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.deleteCompany(99L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(companyRepository, never()).delete(any(Company.class));
    }

    @Test
    void addRecruiter_whenCompanyExistsAndUserNotAlreadyRecruiter_persistsRecruiter() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(savedCompany()));
        when(recruiterRepository.existsByUserIdAndCompanyId(5L, 1L)).thenReturn(false);
        when(recruiterRepository.save(any(Recruiter.class))).thenReturn(savedRecruiter());

        RecruiterDTO dto = RecruiterDTO.builder().userId(5L).designation("Talent Lead").build();
        RecruiterDTO result = companyService.addRecruiter(1L, dto);

        ArgumentCaptor<Recruiter> captor = ArgumentCaptor.forClass(Recruiter.class);
        verify(recruiterRepository).save(captor.capture());
        assertThat(captor.getValue().getCompanyId()).isEqualTo(1L);
        assertThat(result.getId()).isEqualTo(10L);
    }

    @Test
    void addRecruiter_whenCompanyDoesNotExist_throwsResourceNotFoundException() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        RecruiterDTO dto = RecruiterDTO.builder().userId(5L).designation("Talent Lead").build();

        assertThatThrownBy(() -> companyService.addRecruiter(99L, dto))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(recruiterRepository, never()).save(any(Recruiter.class));
    }

    @Test
    void addRecruiter_whenUserAlreadyRecruiterForCompany_throwsDuplicateResourceException() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(savedCompany()));
        when(recruiterRepository.existsByUserIdAndCompanyId(5L, 1L)).thenReturn(true);

        RecruiterDTO dto = RecruiterDTO.builder().userId(5L).designation("Talent Lead").build();

        assertThatThrownBy(() -> companyService.addRecruiter(1L, dto))
                .isInstanceOf(DuplicateResourceException.class);

        verify(recruiterRepository, never()).save(any(Recruiter.class));
    }

    @Test
    void getRecruiters_whenCompanyExists_returnsMappedRecruiters() {
        when(companyRepository.findById(1L)).thenReturn(Optional.of(savedCompany()));
        when(recruiterRepository.findByCompanyId(1L)).thenReturn(List.of(savedRecruiter()));

        List<RecruiterDTO> result = companyService.getRecruiters(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(5L);
    }

    @Test
    void getRecruiters_whenCompanyDoesNotExist_throwsResourceNotFoundException() {
        when(companyRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.getRecruiters(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeRecruiter_whenBelongsToCompany_deletesRecruiter() {
        Recruiter recruiter = savedRecruiter();
        when(recruiterRepository.findById(10L)).thenReturn(Optional.of(recruiter));

        companyService.removeRecruiter(1L, 10L);

        verify(recruiterRepository).delete(recruiter);
    }

    @Test
    void removeRecruiter_whenRecruiterNotFound_throwsResourceNotFoundException() {
        when(recruiterRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> companyService.removeRecruiter(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(recruiterRepository, never()).delete(any(Recruiter.class));
    }

    @Test
    void removeRecruiter_whenRecruiterBelongsToDifferentCompany_throwsResourceNotFoundException() {
        Recruiter recruiter = savedRecruiter();
        when(recruiterRepository.findById(10L)).thenReturn(Optional.of(recruiter));

        assertThatThrownBy(() -> companyService.removeRecruiter(2L, 10L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("does not belong");

        verify(recruiterRepository, never()).delete(any(Recruiter.class));
    }
}
