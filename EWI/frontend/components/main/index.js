// components/main/index.js
export default () => ({
    macroRisks: [
        // Swapped Tailwind classes for raw colors to use with Alpine's :style bindings
        { id: 1, region: 'Sub-Saharan Africa', level: 'Critical', textColor: '#991b1b', bgColor: '#fee2e2', affectedPopulation: '320M' },
        { id: 2, region: 'South Asia', level: 'High', textColor: '#9a3412', bgColor: '#ffedd5', affectedPopulation: '450M' },
        { id: 3, region: 'Latin America', level: 'Moderate', textColor: '#854d0e', bgColor: '#fef9c3', affectedPopulation: '120M' },
        { id: 4, region: 'Europe', level: 'Low', textColor: '#166534', bgColor: '#dcfce7', affectedPopulation: '15M' }
    ],
    
    recentActivity: [
        { id: 101, action: 'New dataset "Global Water Scarcity" uploaded', date: '2 hours ago' },
        { id: 102, action: 'Risk threshold updated for South Asia', date: '5 hours ago' },
        { id: 103, action: 'Sanitation Infrastructure Mapping verified', date: '1 day ago' },
        { id: 104, action: 'System maintenance completed', date: '2 days ago' }
    ]
});