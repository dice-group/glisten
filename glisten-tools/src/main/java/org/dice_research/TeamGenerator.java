package org.dice_research;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class TeamGenerator {
    
    public static final Student[] STUDENTS = new Student[] {
        new Student("Eleni Ilkou", "LUH", true, true),
        new Student("Nikolaos Karalis", "UPB", true, true),
        new Student("Ines Akaichi", "WU", true, false),
        new Student("Pere-Lluis Huguet Cabot", "Babelscape", true, true), // ESR 4
        new Student("Bo Xiong", "USTUTT", true, true),
        new Student("Abelardo Carlos Martinez Lorenzo", "Babelscape", true, true), // ESR 6
        new Student("Umair Qudus", "UPB", true, true),
        new Student("Özge Erten", "MU", true, true),
        new Student("Zubaria Asma", "FORTH", true, true), // ESR 9
        new Student("Cosimo Gregucci", "USTUTT", true, true), // ESR 10
        new Student("N’Dah Jean Kouagou", "UPB", true, true),
        new Student("Marcu Florea", "RUG", true, true),
        new Student("Dawa Chang", "WU", true, true),
        new Student("Maryam Mohammadi", "MU", true, true),
        new Student("Efstratios Koulierakis", "RUG", true, false),
        new Student("Yang Lu", null, false, false),
        new Student("Felipe Vargas", null, false, true),
        new Student("Peb Ruswono Aryan", null, false, true),
        new Student("Sara Moin", null, false, true),
        new Student("Sarah Shoilee", null, false, true),
            };
    public static final int MAX_ESRS_PRO_GROUP = 3;
    public static final int MIN_PER_GROUP = 4;
    public static final int MAX_PER_GROUP = 4;
    public static final int MAX_NON_PROGRAMMERS_PER_GROUP = 1;
    
    public static final int NUMBER_OF_GROUPS = 5;

    public static void main(String[] args) {
        Arrays.sort(STUDENTS, new Comparator<Student>() {
            @Override
            public int compare(Student o1, Student o2) {
                // First, insert people that are not programmers
                if(o1.isProgrammer != o2.isProgrammer) {
                    return o1.isProgrammer ? 1 : -1;
                }
                // Second, insert people that are not ESRs
                if(o1.isESR != o2.isESR) {
                    return o1.isESR ? -1 : 1;
                }
                // In that case, it doesn't matter which student is inserted first
                return 0;
            }
        });
        
        List<List<Student>> groups = new ArrayList<>(NUMBER_OF_GROUPS);
        for (int i = 0; i < NUMBER_OF_GROUPS; ++i) {
            groups.add(new ArrayList<Student>());
        }
        try {
        for(Student student : STUDENTS) {
            insertStudent(groups, student);
        }
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (int i = 0; i < NUMBER_OF_GROUPS; ++i) {
            System.out.println("Group " + i + ":" + groups.get(i).toString());
        }
    }
    
    private static void insertStudent(List<List<Student>> groups, Student student) {
        Optional<List<Student>> group = groups.stream()
                // Only look at groups that are too short
                .filter(g -> g.size() > MIN_PER_GROUP)
                // If the student is no programmer, only look at groups that have only programmers
                .filter(g -> (student.isProgrammer) || (g.stream().filter(s -> (!s.isProgrammer)).count() < MAX_NON_PROGRAMMERS_PER_GROUP))
                // Remove groups that already have a student from the company (if the sutdent has a company)
                .filter(g -> (student.company == null) || (g.stream().filter(s -> student.company.equals(s.company)).count() == 0))
                .findFirst();
        if(group.isEmpty()) {
            group = groups.stream()
                    // Only look at groups that are too short
                    .filter(g -> g.size() < MAX_PER_GROUP)
                    // If the student is no programmer, only look at groups that have only programmers
                    .filter(g -> (student.isProgrammer) || (g.stream().filter(s -> (!s.isProgrammer)).count() < MAX_NON_PROGRAMMERS_PER_GROUP))
                    // If the student is no programmer, only look at groups that have only programmers
                    .filter(g -> (!student.isESR) || (g.stream().filter(s -> s.isESR).count() < MAX_ESRS_PRO_GROUP))
                    // Remove groups that already have a student from the company (if the sutdent has a company)
                    .filter(g -> (student.company == null) || (g.stream().filter(s -> student.company.equals(s.company)).count() == 0))
                    .findFirst();
        }
        if(group.isEmpty()) {
            throw new IllegalStateException("Couldn't find group for " + student.toString());
        } else {
            group.get().add(student);
        }
    }

    public static class Student {
        protected String name;
        protected String company;
        protected boolean isESR;
        protected boolean isProgrammer;
        
        public Student(String name, String company, boolean isESR, boolean isProgrammer) {
            this.name = name;
            this.company = company;
            this.isESR = isESR;
            this.isProgrammer = isProgrammer;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
