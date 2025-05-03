package local.ytk.skillsmod.skills;

public interface HasSkills {
    SkillList getSkills();
    void setSkills(SkillList skills);
    void updateSkills(SkillList skillList);
}
