package it.polito.ai.es2.entities;

import it.polito.ai.es2.exceptions.TeamServiceException;
import it.polito.ai.es2.exceptions.TooManyVmInstancesException;
import it.polito.ai.es2.utility.TeamStatus;
import it.polito.ai.es2.utility.VmStatus;
import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
@Builder
public class Team {

    @Id
    @GeneratedValue
    private Long id;

    @NonNull
    @Column(nullable = false)
    private String name;

    private TeamStatus status = TeamStatus.PENDING;

    @NonNull
    @Column(nullable = false)
    private int vcpuMAX;

    @NonNull
    @Column(nullable = false)
    private float memoryMAX;

    @NonNull
    @Column(nullable = false)
    private float diskMAX;

    private int maxVmInstance = 0;

    private int maxRunningVmInstance = 0;

    @NonNull
    @ManyToOne
    @JoinColumn(name = "course_id")
    private Course course;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "student_team",
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "student_id"))
    private List<Student> members = new ArrayList<>();

    @OneToMany(mappedBy = "team", cascade = CascadeType.REMOVE)
    private List<VmInstance> vmInstances = new ArrayList<>();

    public boolean setCourse(Course course) {
        if (this.course == course)
            return false;

        if (this.course != null)
            this.course.getTeams().remove(this);

        if (course != null && !course.getTeams().contains(this))
            course.getTeams().add(this);

        this.course = course;
        return true;
    }

    public boolean addMember(Student student) {
        if (this.members.contains(student))
            return false;
        else {
            this.members.add(student);
            student.getTeams().add(this);
            return true;
        }
    }

    public boolean removeMember(Student student) {
        if (this.members.contains(student)) {
            this.members.remove(student);
            student.getTeams().remove(this);
            return true;
        } else
            return false;
    }

    public boolean addVmInstanceToTeam(VmInstance vmInstance) {
        int totCpu = vmInstance.getVcpu();
        float totMemory = vmInstance.getMemory();
        float totDisk = vmInstance.getDisk();
        int activeVm=0;
        for (VmInstance var:
                vmInstances) {
            totCpu+=var.getVcpu();
            totMemory+=var.getMemory();
            totDisk+=var.getDisk();
            if(var.getStatus()==VmStatus.RUNNING)
                activeVm++;
        }
        if(totCpu <= vcpuMAX && totMemory <= memoryMAX && totDisk <= diskMAX)
        {
            if((vmInstances.size()+1) > maxVmInstance) throw new TooManyVmInstancesException("Too many vm instances in this team");
            if(activeVm + 1 > maxRunningVmInstance) throw new TooManyVmInstancesException("Too many vm instances running in this team");
            vmInstances.add(vmInstance);
            maxVmInstance++;
            vmInstance.setTeam(this);
            return true;
        }
        return false;
    }
    // todo continuare da qui
    public boolean changeStatusVm(VmInstance vmInstance, VmStatus vmStatus) {
        int activeVm=0;
        if(vmStatus == VmStatus.RUNNING && vmInstance.getStatus() == VmStatus.SUSPENDED) {
            for (VmInstance var:
                    vmInstances) {
                if(var.getStatus()==VmStatus.RUNNING)
                    activeVm++;
            }
            if (activeVm+1 > maxRunningVmInstance) throw new TooManyVmInstancesException("Too many vm instances running in this team");
            else vmInstance.setStatus(vmStatus);
            return true;
        }

        if(vmStatus == VmStatus.SUSPENDED && vmInstance.getStatus() == VmStatus.RUNNING) {
            vmInstance.setStatus(vmStatus);
            return true;
        }
        throw new TeamServiceException("Command not correct, check vm status");
    }

    public boolean removeVmInstance(VmInstance vmInstance, Student student) {
        if(!vmInstance.getOwners().contains(student)) throw new TeamServiceException("Permission denied");
        if (vmInstance.getStatus() == VmStatus.SUSPENDED)
        {
            vmInstances.remove(vmInstance);
            vmInstance.getCreator().removeCreatedVm(vmInstance);
            List<Student> owners = vmInstance.getOwners();
            vmInstance.getOwners().stream().forEach(s -> s.removeOwnedVm(vmInstance));
            return true;
        }
        throw new TeamServiceException("Virtual Machine must be suspended");

    }
}
