#!/usr/bin/env node

import { Command } from 'commander';
import chalk from 'chalk';
import fs from 'fs-extra';
import path from 'path';
import { execSync } from 'child_process';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const packageRoot = path.resolve(__dirname, '..');

const program = new Command();

// Helper function to find Logseq graph directory
function findLogseqGraph(graphName) {
  const possiblePaths = [
    path.join(process.env.HOME, '.logseq', 'graphs', `logseq_local_${graphName.replace(/[^a-zA-Z0-9]/g, '+')}.transit`),
    path.join(process.env.HOME, '.logseq', 'graphs', `${graphName}.transit`),
  ];
  
  for (const graphPath of possiblePaths) {
    if (fs.existsSync(graphPath)) {
      return graphPath;
    }
  }
  return null;
}

// Helper function to list available queries
function listQueries() {
  const queriesDir = path.join(packageRoot, 'templates', 'queries');
  const customQueriesDir = path.join(process.cwd(), 'queries');
  
  console.log(chalk.blue('\n📋 Available Query Templates:'));
  
  // List template queries
  if (fs.existsSync(queriesDir)) {
    const templates = fs.readdirSync(queriesDir).filter(f => f.endsWith('.edn'));
    templates.forEach(template => {
      console.log(`  ${chalk.green('template:')} ${template.replace('.edn', '')}`);
    });
  }
  
  // List custom queries in current directory
  if (fs.existsSync(customQueriesDir)) {
    const custom = fs.readdirSync(customQueriesDir).filter(f => f.endsWith('.edn'));
    if (custom.length > 0) {
      console.log(chalk.blue('\n📁 Custom Queries in ./queries/:'));
      custom.forEach(query => {
        console.log(`  ${chalk.yellow('custom:')} ${query.replace('.edn', '')}`);
      });
    }
  }
}

program
  .name('logseq-query')
  .description('Extract data from Logseq using DataScript queries and convert to markdown')
  .version('1.0.0');

program
  .command('run')
  .description('Run a query and generate markdown')
  .argument('<query>', 'Query file name (without .edn) or path to .edn file')
  .option('-g, --graph <name>', 'Logseq graph name', 'yihan_main_LOGSEQ')
  .option('-o, --output <name>', 'Output file name (without extension)')
  .option('--output-dir <dir>', 'Output directory', './output')
  .action(async (queryArg, options) => {
    try {
      console.log(chalk.blue('🚀 Starting Logseq Query to Markdown Pipeline'));
      
      // Determine query file path
      let queryFile;
      if (queryArg.endsWith('.edn')) {
        queryFile = path.resolve(queryArg);
      } else {
        // Look for template first, then custom
        const templatePath = path.join(packageRoot, 'templates', 'queries', `${queryArg}.edn`);
        const customPath = path.join(process.cwd(), 'queries', `${queryArg}.edn`);
        
        if (fs.existsSync(templatePath)) {
          queryFile = templatePath;
          console.log(chalk.green(`📋 Using template query: ${queryArg}`));
        } else if (fs.existsSync(customPath)) {
          queryFile = customPath;
          console.log(chalk.green(`📁 Using custom query: ${queryArg}`));
        } else {
          console.error(chalk.red(`❌ Query not found: ${queryArg}`));
          console.log(chalk.yellow('\nTip: Use "logseq-query list" to see available queries'));
          process.exit(1);
        }
      }
      
      if (!fs.existsSync(queryFile)) {
        console.error(chalk.red(`❌ Query file not found: ${queryFile}`));
        process.exit(1);
      }
      
      // Verify graph exists
      const graphPath = findLogseqGraph(options.graph);
      if (!graphPath) {
        console.error(chalk.red(`❌ Logseq graph not found: ${options.graph}`));
        console.log(chalk.yellow('💡 Make sure Logseq has opened this graph at least once'));
        process.exit(1);
      }
      
      console.log(chalk.green(`✅ Found graph: ${graphPath}`));
      
      // Set up output paths
      const outputName = options.output || path.basename(queryFile, '.edn');
      fs.ensureDirSync(options.outputDir);
      
      const ednOutput = path.join(options.outputDir, `${outputName}_data.edn`);
      const mdOutput = path.join(options.outputDir, `${outputName}_analysis.md`);
      
      console.log(chalk.blue(`📄 EDN output: ${ednOutput}`));
      console.log(chalk.blue(`📝 Markdown output: ${mdOutput}`));
      
      // Run the extraction script
      const scriptPath = path.join(packageRoot, 'src', 'shell', 'query_to_markdown.sh');
      const command = `bash "${scriptPath}" "${queryFile}" "${outputName}"`;
      
      console.log(chalk.blue('\n⚙️  Running extraction...'));
      execSync(command, { 
        cwd: packageRoot,
        stdio: 'inherit',
        env: { 
          ...process.env, 
          OUTPUT_DIR: options.outputDir,
          GRAPH_NAME: options.graph
        }
      });
      
      console.log(chalk.green('\n🎉 Pipeline completed successfully!'));
      console.log(chalk.blue(`\n📖 View results:\n  cat "${mdOutput}"`));
      
    } catch (error) {
      console.error(chalk.red(`❌ Error: ${error.message}`));
      process.exit(1);
    }
  });

program
  .command('list')
  .description('List available queries')
  .action(() => {
    listQueries();
  });

program
  .command('init')
  .description('Initialize a new query project in current directory')
  .action(() => {
    const queriesDir = path.join(process.cwd(), 'queries');
    const outputDir = path.join(process.cwd(), 'output');
    
    fs.ensureDirSync(queriesDir);
    fs.ensureDirSync(outputDir);
    
    // Copy example queries
    const templatesDir = path.join(packageRoot, 'templates', 'queries');
    if (fs.existsSync(templatesDir)) {
      fs.copySync(templatesDir, queriesDir);
    }
    
    console.log(chalk.green('✅ Initialized logseq-query project!'));
    console.log(chalk.blue(`📁 Created: ${queriesDir}`));
    console.log(chalk.blue(`📁 Created: ${outputDir}`));
    console.log(chalk.yellow('\n💡 Edit queries in ./queries/ and run with: logseq-query run <query-name>'));
  });

program
  .command('create')
  .description('Create a new query from template')
  .argument('<name>', 'Name for the new query')
  .option('-t, --template <type>', 'Template type (references, content, blocks)', 'references')
  .action((name, options) => {
    const queriesDir = path.join(process.cwd(), 'queries');
    fs.ensureDirSync(queriesDir);
    
    const templates = {
      references: `[:find (pull ?b [*])
 :where
 [?t :block/name "${name}"]  ; Find the page/tag with name "${name}"
 [?b :block/refs ?t]]        ; Find blocks that reference it`,
      
      content: `[:find (pull ?b [*])
 :where
 [?b :block/content ?content]
 [(clojure.string/includes? ?content "${name}")]]`,
      
      blocks: `[:find (pull ?b [*])
 :where
 [?p :block/name "${name}"]  ; Find the page
 [?b :block/page ?p]]        ; Find all blocks on that page`
    };
    
    const queryContent = templates[options.template] || templates.references;
    const queryFile = path.join(queriesDir, `${name}.edn`);
    
    fs.writeFileSync(queryFile, queryContent);
    
    console.log(chalk.green(`✅ Created query: ${queryFile}`));
    console.log(chalk.blue(`📝 Template: ${options.template}`));
    console.log(chalk.yellow(`\n💡 Run with: logseq-query run ${name}`));
  });

program.parse(); 